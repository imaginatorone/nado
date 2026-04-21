package com.solarl.nado.security;

import com.solarl.nado.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.security.Key;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private static final String SECRET = "TestSecretKeyForJWTTokenGeneration2026VeryLongAndSecure256bits!!";
    private static final long EXPIRATION_MS = 86400000;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret(SECRET);
        props.setExpirationMs(EXPIRATION_MS);
        tokenProvider = new JwtTokenProvider(props);
        tokenProvider.init();
    }

    @Test
    @DisplayName("Генерация токена: содержит userId в subject")
    void generateToken_containsUserId() {
        UserDetailsImpl userDetails = new UserDetailsImpl(
                42L, "Test", "test@test.com", "hash", "USER", true,
                Collections.emptyList());
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        String token = tokenProvider.generateToken(authentication);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("Извлечение userId из токена: корректный ID")
    void getUserIdFromToken_returnsCorrectId() {
        UserDetailsImpl userDetails = new UserDetailsImpl(
                42L, "Test", "test@test.com", "hash", "USER", true,
                Collections.emptyList());
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        String token = tokenProvider.generateToken(authentication);

        Long userId = tokenProvider.getUserIdFromToken(token);

        assertEquals(42L, userId);
    }

    @Test
    @DisplayName("Валидация токена: валидный токен — true")
    void validateToken_validToken_returnsTrue() {
        UserDetailsImpl userDetails = new UserDetailsImpl(
                1L, "Test", "test@test.com", "hash", "USER", true,
                Collections.emptyList());
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        String token = tokenProvider.generateToken(authentication);

        assertTrue(tokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("Валидация токена: невалидный токен — false")
    void validateToken_invalidToken_returnsFalse() {
        assertFalse(tokenProvider.validateToken("invalid.token.here"));
    }

    @Test
    @DisplayName("Валидация токена: истекший токен — false")
    void validateToken_expiredToken_returnsFalse() {
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        String expiredToken = Jwts.builder()
                .setSubject("1")
                .setIssuedAt(new Date(System.currentTimeMillis() - 200000))
                .setExpiration(new Date(System.currentTimeMillis() - 100000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        assertFalse(tokenProvider.validateToken(expiredToken));
    }
}
