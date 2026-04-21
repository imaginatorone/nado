package com.solarl.nado.security;

import com.solarl.nado.config.AuthModeProperties;
import com.solarl.nado.entity.User;
import com.solarl.nado.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

/**
 * dual-mode аутентификация на уровне STOMP CONNECT.
 * извлекает token из заголовка Authorization, проверяет его
 * через текущий auth mode (legacy JWT / Keycloak JWT),
 * и устанавливает principal для всех последующих фреймов сессии.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final AuthModeProperties authModeProperties;
    private final JwtTokenProvider legacyJwtProvider;
    private final JwtDecoder jwtDecoder;
    private final UserRepository userRepository;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> authHeaders = accessor.getNativeHeader("Authorization");
            if (authHeaders == null || authHeaders.isEmpty()) {
                log.debug("WS_CONNECT: нет Authorization header");
                return message;
            }

            String token = authHeaders.get(0);
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            Principal principal = authenticate(token);
            if (principal != null) {
                accessor.setUser(principal);
                log.debug("WS_CONNECT: authenticated userId={}", principal.getName());
            }
        }

        return message;
    }

    private Principal authenticate(String token) {
        try {
            if (authModeProperties.isKeycloak()) {
                return authenticateKeycloak(token);
            } else {
                return authenticateLegacy(token);
            }
        } catch (Exception e) {
            log.warn("WS_AUTH_FAILED: {}", e.getMessage());
            return null;
        }
    }

    private Principal authenticateLegacy(String token) {
        if (!legacyJwtProvider.validateToken(token)) return null;
        Long userId = legacyJwtProvider.getUserIdFromToken(token);
        UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserById(userId);
        return new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
    }

    private Principal authenticateKeycloak(String token) {
        Jwt jwt = jwtDecoder.decode(token);
        String email = jwt.getClaimAsString("email");
        if (email == null) return null;

        // используем userId как principal name для маршрутизации сообщений
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return null;

        return new UsernamePasswordAuthenticationToken(
                user.getId().toString(), null, Collections.emptyList());
    }
}
