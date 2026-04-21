package com.solarl.nado.service;

import com.solarl.nado.config.MailNotificationProperties;
import com.solarl.nado.entity.Notification.NotificationType;
import com.solarl.nado.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

    @Mock private JavaMailSender mailSender;
    @Mock private MailNotificationProperties mailProperties;

    @InjectMocks private EmailNotificationService emailService;

    private User verifiedUser;
    private User unverifiedUser;
    private Map<String, Object> payload;

    @BeforeEach
    void setUp() {
        verifiedUser = User.builder()
                .id(1L).name("Иван").email("ivan@test.com")
                .emailVerified(true).build();
        unverifiedUser = User.builder()
                .id(2L).name("Петр").email("petr@test.com")
                .emailVerified(false).build();
        payload = Map.of("adId", 10L, "adTitle", "Тестовое объявление");
    }

    @Test
    @DisplayName("verified user + supported type -> email sent")
    void trySend_verifiedAndSupported_sends() {
        when(mailProperties.isEnabled()).thenReturn(true);
        when(mailProperties.getFrom()).thenReturn("noreply@nado.ru");

        emailService.trySend(verifiedUser, NotificationType.AD_APPROVED, payload);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("unverified user -> email skipped")
    void trySend_unverified_skipped() {
        when(mailProperties.isEnabled()).thenReturn(true);

        emailService.trySend(unverifiedUser, NotificationType.AD_APPROVED, payload);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("mail disabled -> email skipped")
    void trySend_disabled_skipped() {
        when(mailProperties.isEnabled()).thenReturn(false);

        emailService.trySend(verifiedUser, NotificationType.AD_APPROVED, payload);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("unsupported type -> email skipped (e.g. NEW_MESSAGE)")
    void trySend_unsupportedType_skipped() {
        when(mailProperties.isEnabled()).thenReturn(true);

        emailService.trySend(verifiedUser, NotificationType.NEW_MESSAGE, payload);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("SMTP failure -> app flow survives, no exception thrown")
    void trySend_smtpFailure_survives() {
        when(mailProperties.isEnabled()).thenReturn(true);
        when(mailProperties.getFrom()).thenReturn("noreply@nado.ru");
        doThrow(new MailSendException("SMTP connection refused"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // не должен кидать исключение
        emailService.trySend(verifiedUser, NotificationType.AD_APPROVED, payload);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("all MVP types are sent when enabled and verified")
    void trySend_allMvpTypes() {
        when(mailProperties.isEnabled()).thenReturn(true);
        when(mailProperties.getFrom()).thenReturn("noreply@nado.ru");

        NotificationType[] mvpTypes = {
                NotificationType.AD_APPROVED,
                NotificationType.AD_REJECTED,
                NotificationType.WANTED_MATCH,
                NotificationType.AUCTION_WON,
                NotificationType.AUCTION_OUTBID,
                NotificationType.AUCTION_NO_BIDS,
        };

        for (NotificationType type : mvpTypes) {
            emailService.trySend(verifiedUser, type, payload);
        }

        verify(mailSender, times(mvpTypes.length)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("null email -> skipped")
    void trySend_nullEmail_skipped() {
        User noEmail = User.builder().id(3L).emailVerified(true).build();
        when(mailProperties.isEnabled()).thenReturn(true);

        emailService.trySend(noEmail, NotificationType.AD_APPROVED, payload);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
