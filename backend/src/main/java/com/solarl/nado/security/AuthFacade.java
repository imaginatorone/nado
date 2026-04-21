package com.solarl.nado.security;

import com.solarl.nado.entity.User;
import com.solarl.nado.exception.ResourceNotFoundException;
import com.solarl.nado.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * единственный вход в identity-context приложения.
 * ни контроллеры, ни сервисы не должны разбирать JWT/SecurityContext напрямую.
 * поддерживает legacy (UserDetailsImpl) и keycloak (Jwt) principal.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthFacade {

    private final UserRepository userRepository;

    public Long getCurrentUserId() {
        Object principal = getAuthentication().getPrincipal();

        if (principal instanceof UserDetailsImpl) {
            return ((UserDetailsImpl) principal).getId();
        } else if (principal instanceof Jwt) {
            return getOrCreateNadoUser((Jwt) principal).getId();
        }

        throw new IllegalStateException("неизвестный тип principal: " + principal.getClass());
    }

    public String getCurrentEmail() {
        Object principal = getAuthentication().getPrincipal();

        if (principal instanceof UserDetailsImpl) {
            return ((UserDetailsImpl) principal).getEmail();
        } else if (principal instanceof Jwt) {
            return ((Jwt) principal).getClaimAsString("email");
        }

        throw new IllegalStateException("неизвестный тип principal");
    }

    public String getCurrentRole() {
        Object principal = getAuthentication().getPrincipal();

        if (principal instanceof UserDetailsImpl) {
            return ((UserDetailsImpl) principal).getRole();
        } else if (principal instanceof Jwt) {
            List<String> roles = extractKeycloakRoles((Jwt) principal);
            if (roles.contains("ADMIN")) return "ADMIN";
            if (roles.contains("MODERATOR")) return "MODERATOR";
            return "USER";
        }

        return "USER";
    }

    public Optional<String> getKeycloakUserId() {
        Object principal = getAuthentication().getPrincipal();
        if (principal instanceof Jwt) {
            return Optional.ofNullable(((Jwt) principal).getSubject());
        }
        return Optional.empty();
    }

    public boolean isEmailVerifiedInIdP() {
        Object principal = getAuthentication().getPrincipal();
        if (principal instanceof Jwt) {
            return Boolean.TRUE.equals(((Jwt) principal).getClaim("email_verified"));
        }
        return false;
    }

    public User getCurrentNadoUser() {
        Object principal = getAuthentication().getPrincipal();

        if (principal instanceof UserDetailsImpl) {
            Long userId = ((UserDetailsImpl) principal).getId();
            return userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("пользователь не найден"));
        } else if (principal instanceof Jwt) {
            return getOrCreateNadoUser((Jwt) principal);
        }

        throw new IllegalStateException("неизвестный тип principal");
    }

    /**
     * safe account linking: keycloak → nado.
     * правила:
     * - keycloakUserId → immutable после привязки
     * - email-based linking только если аккаунт не привязан к другому KC user
     * - заблокированный аккаунт не линкуется
     * - новый пользователь создаётся автоматически при первом входе
     */
    private User getOrCreateNadoUser(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        Boolean emailVerified = jwt.getClaim("email_verified");

        String name = jwt.getClaimAsString("preferred_username");
        if (name == null || name.isBlank()) {
            name = jwt.getClaimAsString("given_name");
        }
        if (name == null || name.isBlank()) {
            name = email != null ? email.split("@")[0] : "User";
        }

        // 1. поиск по keycloakUserId
        Optional<User> byKcId = userRepository.findByKeycloakUserId(keycloakId);
        if (byKcId.isPresent()) {
            User user = byKcId.get();
            syncEmailVerified(user, emailVerified);
            return user;
        }

        // 2. поиск по email — safe linking
        if (email != null) {
            Optional<User> byEmail = userRepository.findByEmail(email);
            if (byEmail.isPresent()) {
                User user = byEmail.get();

                if (user.getKeycloakUserId() != null && !user.getKeycloakUserId().equals(keycloakId)) {
                    log.warn("SECURITY: email {} уже привязан к другому KC user, отказ в auto-link", email);
                    throw new IllegalStateException("аккаунт с этим email уже привязан к другому пользователю");
                }
                if (Boolean.TRUE.equals(user.getBanned())) {
                    log.warn("SECURITY: заблокированный пользователь {} пытается войти через Keycloak", email);
                    throw new IllegalStateException("аккаунт заблокирован");
                }

                user.setKeycloakUserId(keycloakId);
                user.setAuthProvider(detectAuthProvider(jwt));
                syncEmailVerified(user, emailVerified);
                user = userRepository.save(user);
                log.info("привязан Nado user id={} к Keycloak id={}", user.getId(), keycloakId);
                return user;
            }
        }

        // 3. auto-provision
        List<String> roles = extractKeycloakRoles(jwt);
        User.Role role = User.Role.USER;
        if (roles.contains("ADMIN")) role = User.Role.ADMIN;
        else if (roles.contains("MODERATOR")) role = User.Role.MODERATOR;

        User newUser = User.builder()
                .name(name)
                .email(email != null ? email : keycloakId + "@keycloak.local")
                .passwordHash("KEYCLOAK_MANAGED")
                .keycloakUserId(keycloakId)
                .authProvider(detectAuthProvider(jwt))
                .role(role)
                .active(true)
                .emailVerified(Boolean.TRUE.equals(emailVerified))
                .build();
        newUser = userRepository.save(newUser);
        log.info("создан Nado user id={} из Keycloak id={}", newUser.getId(), keycloakId);
        return newUser;
    }

    private void syncEmailVerified(User user, Boolean keycloakEmailVerified) {
        // источник истины для emailVerified — Keycloak
        if (Boolean.TRUE.equals(keycloakEmailVerified) && !Boolean.TRUE.equals(user.getEmailVerified())) {
            user.setEmailVerified(true);
            userRepository.save(user);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> extractKeycloakRoles(Jwt jwt) {
        // стандартный путь: realm_access.roles
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null) {
            Object roles = realmAccess.get("roles");
            if (roles instanceof List) {
                return ((List<String>) roles).stream()
                        .map(String::toUpperCase)
                        .collect(Collectors.toList());
            }
        }
        // fallback: кастомный claim из realm mapper
        List<String> realmRoles = jwt.getClaim("realm_roles");
        if (realmRoles != null) {
            return realmRoles.stream().map(String::toUpperCase).collect(Collectors.toList());
        }
        return List.of("USER");
    }

    private User.AuthProvider detectAuthProvider(Jwt jwt) {
        String idp = jwt.getClaimAsString("identity_provider");
        if ("google".equalsIgnoreCase(idp)) return User.AuthProvider.GOOGLE;
        if ("vk".equalsIgnoreCase(idp)) return User.AuthProvider.VK;
        return User.AuthProvider.LOCAL;
    }

    private Authentication getAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("нет аутентификации в SecurityContext");
        }
        return auth;
    }
}
