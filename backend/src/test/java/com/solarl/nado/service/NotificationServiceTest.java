package com.solarl.nado.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solarl.nado.entity.Notification;
import com.solarl.nado.entity.Notification.NotificationType;
import com.solarl.nado.entity.User;
import com.solarl.nado.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository repository;
    @Spy  private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private NotificationService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).name("Иван").email("ivan@test.com").build();
    }

    @Test
    @DisplayName("send() создаёт in-app уведомление")
    void send_createsNotification() {
        when(repository.existsByUserIdAndTypeAndDedupKey(eq(1L), any(), anyString())).thenReturn(false);

        service.send(user, NotificationType.AD_APPROVED, Map.of("adId", 10L, "adTitle", "Тест"));

        verify(repository).save(argThat(n ->
                n.getUser().getId().equals(1L)
                && n.getType() == NotificationType.AD_APPROVED
                && n.getPayload().contains("Тест")
        ));
    }

    @Test
    @DisplayName("dedup: повторное уведомление не создаётся")
    void send_dedup() {
        when(repository.existsByUserIdAndTypeAndDedupKey(eq(1L), any(), anyString())).thenReturn(true);

        service.send(user, NotificationType.AD_APPROVED, Map.of("adId", 10L, "adTitle", "Тест"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("notifyAdRejected: convenience-метод работает")
    void notifyAdRejected() {
        when(repository.existsByUserIdAndTypeAndDedupKey(eq(1L), any(), anyString())).thenReturn(false);

        service.notifyAdRejected(user, 5L, "Плохое фото", "нарушение правил");

        verify(repository).save(argThat(n ->
                n.getType() == NotificationType.AD_REJECTED
                && n.getPayload().contains("нарушение правил")
        ));
    }

    @Test
    @DisplayName("notifyAuctionOutbid: уведомление перебитого")
    void notifyAuctionOutbid() {
        when(repository.existsByUserIdAndTypeAndDedupKey(eq(1L), any(), anyString())).thenReturn(false);

        service.notifyAuctionOutbid(user, 3L, 10L, "iPhone 15");

        verify(repository).save(argThat(n ->
                n.getType() == NotificationType.AUCTION_OUTBID
                && n.getPayload().contains("iPhone 15")
        ));
    }

    @Test
    @DisplayName("notifyWantedMatch: уведомление о совпадении")
    void notifyWantedMatch() {
        when(repository.existsByUserIdAndTypeAndDedupKey(eq(1L), any(), anyString())).thenReturn(false);

        service.notifyWantedMatch(user, 1L, 20L, "Велосипед Trek");

        verify(repository).save(argThat(n ->
                n.getType() == NotificationType.WANTED_MATCH
                && n.getPayload().contains("Велосипед Trek")
        ));
    }
}
