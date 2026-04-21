package com.solarl.nado.service;

import com.solarl.nado.entity.PhoneVerification;
import com.solarl.nado.entity.User;
import com.solarl.nado.repository.PhoneVerificationRepository;
import com.solarl.nado.repository.UserRepository;
import com.solarl.nado.security.AuthFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhoneVerificationServiceTest {

    @Mock private PhoneVerificationRepository verificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private SmsVerificationService smsSender;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthFacade authFacade;

    @InjectMocks
    private PhoneVerificationService service;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).name("Тест").email("test@test.com")
                .phone("+79001234567").phoneVerified(false).build();
    }

    @Test
    @DisplayName("requestCode: создаёт OTP с хэшем и отправляет SMS")
    void requestCode_success() {
        when(authFacade.getCurrentUserId()).thenReturn(1L);
        when(verificationRepository.countByUserIdAndCreatedAtAfter(eq(1L), any())).thenReturn(0L);
        when(verificationRepository.findTopByUserIdAndPhoneAndVerifiedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                eq(1L), eq("+79001234567"), any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$hashed");
        when(verificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.requestCode("+79001234567");

        verify(smsSender).sendCode(eq("+79001234567"), anyString());
        ArgumentCaptor<PhoneVerification> captor = ArgumentCaptor.forClass(PhoneVerification.class);
        verify(verificationRepository).save(captor.capture());
        assertEquals("$2a$hashed", captor.getValue().getCodeHash());
    }

    @Test
    @DisplayName("requestCode: rate limit — больше 5 запросов в час")
    void requestCode_rateLimited() {
        when(authFacade.getCurrentUserId()).thenReturn(1L);
        when(verificationRepository.countByUserIdAndCreatedAtAfter(eq(1L), any())).thenReturn(5L);

        assertThrows(IllegalStateException.class,
                () -> service.requestCode("+79001234567"));
        verify(smsSender, never()).sendCode(any(), any());
    }

    @Test
    @DisplayName("requestCode: cooldown — повторный запрос менее минуты назад")
    void requestCode_cooldown() {
        PhoneVerification recent = PhoneVerification.builder()
                .createdAt(LocalDateTime.now().minusSeconds(30))
                .expiresAt(LocalDateTime.now().plusMinutes(4))
                .build();

        when(authFacade.getCurrentUserId()).thenReturn(1L);
        when(verificationRepository.countByUserIdAndCreatedAtAfter(eq(1L), any())).thenReturn(1L);
        when(verificationRepository.findTopByUserIdAndPhoneAndVerifiedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                eq(1L), eq("+79001234567"), any())).thenReturn(Optional.of(recent));

        assertThrows(IllegalStateException.class,
                () -> service.requestCode("+79001234567"));
    }

    @Test
    @DisplayName("verifyCode: правильный код — верифицирует пользователя")
    void verifyCode_success() {
        PhoneVerification verification = PhoneVerification.builder()
                .userId(1L).phone("+79001234567")
                .codeHash("$2a$hashed").attempts(0)
                .verified(false).expiresAt(LocalDateTime.now().plusMinutes(3))
                .build();

        when(authFacade.getCurrentUserId()).thenReturn(1L);
        when(verificationRepository.findTopByUserIdAndPhoneAndVerifiedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                eq(1L), eq("+79001234567"), any())).thenReturn(Optional.of(verification));
        when(passwordEncoder.matches("123456", "$2a$hashed")).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(verificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean result = service.verifyCode("+79001234567", "123456");

        assertTrue(result);
        assertTrue(testUser.getPhoneVerified());
        assertNotNull(testUser.getPhoneVerifiedAt());
    }

    @Test
    @DisplayName("verifyCode: неправильный код — инкремент attempts")
    void verifyCode_wrongCode() {
        PhoneVerification verification = PhoneVerification.builder()
                .userId(1L).phone("+79001234567")
                .codeHash("$2a$hashed").attempts(0)
                .verified(false).expiresAt(LocalDateTime.now().plusMinutes(3))
                .build();

        when(authFacade.getCurrentUserId()).thenReturn(1L);
        when(verificationRepository.findTopByUserIdAndPhoneAndVerifiedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                eq(1L), eq("+79001234567"), any())).thenReturn(Optional.of(verification));
        when(passwordEncoder.matches("000000", "$2a$hashed")).thenReturn(false);
        when(verificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean result = service.verifyCode("+79001234567", "000000");

        assertFalse(result);
        assertEquals(1, verification.getAttempts());
    }

    @Test
    @DisplayName("verifyCode: превышены попытки — ошибка")
    void verifyCode_maxAttempts() {
        PhoneVerification verification = PhoneVerification.builder()
                .userId(1L).phone("+79001234567")
                .codeHash("$2a$hashed").attempts(5)
                .verified(false).expiresAt(LocalDateTime.now().plusMinutes(3))
                .build();

        when(authFacade.getCurrentUserId()).thenReturn(1L);
        when(verificationRepository.findTopByUserIdAndPhoneAndVerifiedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                eq(1L), eq("+79001234567"), any())).thenReturn(Optional.of(verification));

        assertThrows(IllegalStateException.class,
                () -> service.verifyCode("+79001234567", "123456"));
    }
}
