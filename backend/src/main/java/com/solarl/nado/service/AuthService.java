package com.solarl.nado.service;

import com.solarl.nado.dto.request.LoginRequest;
import com.solarl.nado.dto.request.RegisterRequest;
import com.solarl.nado.dto.response.AuthResponse;
import com.solarl.nado.entity.User;
import com.solarl.nado.repository.UserRepository;
import com.solarl.nado.security.JwtTokenProvider;
import com.solarl.nado.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Попытка регистрации пользователя с email: {}", request.getEmail());
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            log.warn("Регистрация провалена: пароли не совпадают для email={}", request.getEmail());
            throw new IllegalArgumentException("Пароли не совпадают");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Регистрация провалена: email уже существует: {}", request.getEmail());
            throw new IllegalArgumentException("Пользователь с таким email уже существует");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .active(true)
                .build();

        userRepository.save(user);
        log.info("Пользователь зарегистрирован: id={}, email={}", user.getId(), user.getEmail());

        // автоматический вход после регистрации
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.generateToken(authentication);
        return new AuthResponse(jwt, user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Попытка входа: email={}", request.getEmail());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.generateToken(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        return new AuthResponse(jwt, userDetails.getId(), userDetails.getName(),
                userDetails.getEmail(), userDetails.getRole());
    }
}
