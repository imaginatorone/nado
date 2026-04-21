package com.solarl.nado.service;

import com.solarl.nado.dto.request.LoginRequest;
import com.solarl.nado.dto.request.RegisterRequest;
import com.solarl.nado.dto.response.AuthResponse;
import com.solarl.nado.entity.User;
import com.solarl.nado.repository.UserRepository;
import com.solarl.nado.security.JwtTokenProvider;
import com.solarl.nado.security.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setName("Иван Иванов");
        registerRequest.setEmail("ivan@example.com");
        registerRequest.setPhone("+79001234567");
        registerRequest.setPassword("secret123");
        registerRequest.setConfirmPassword("secret123");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("ivan@example.com");
        loginRequest.setPassword("secret123");
    }

    @Test
    @DisplayName("Регистрация: успешная регистрация нового пользователя")
    void register_success() {
        // подготовка
        when(userRepository.existsByEmail("ivan@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn("jwt-token");

        // выполнение
        AuthResponse response = authService.register(registerRequest);

        // проверка
        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals(1L, response.getUserId());
        assertEquals("Иван Иванов", response.getName());
        assertEquals("ivan@example.com", response.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Регистрация: пароли не совпадают — ошибка")
    void register_passwordsMismatch_throwsException() {
        // подготовка
        registerRequest.setConfirmPassword("different");

        // выполнение и проверка
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(registerRequest));
        assertEquals("Пароли не совпадают", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Регистрация: дублирующий email — ошибка")
    void register_duplicateEmail_throwsException() {
        // подготовка
        when(userRepository.existsByEmail("ivan@example.com")).thenReturn(true);

        // выполнение и проверка
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(registerRequest));
        assertEquals("Пользователь с таким email уже существует", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Вход: успешный логин")
    void login_success() {
        // подготовка
        Authentication authentication = mock(Authentication.class);
        UserDetailsImpl userDetails = new UserDetailsImpl(
                1L, "Иван Иванов", "ivan@example.com", "$2a$hashed", "USER", true,
                Collections.emptyList());
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn("jwt-token");

        // выполнение
        AuthResponse response = authService.login(loginRequest);

        // проверка
        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("Иван Иванов", response.getName());
    }

    @Test
    @DisplayName("Вход: неверный пароль — ошибка")
    void login_badCredentials_throwsException() {
        // подготовка
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // выполнение и проверка
        assertThrows(BadCredentialsException.class,
                () -> authService.login(loginRequest));
    }
}
