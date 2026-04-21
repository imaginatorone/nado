package com.solarl.nado.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solarl.nado.dto.response.NotificationResponse;
import com.solarl.nado.entity.Notification;
import com.solarl.nado.entity.Notification.NotificationType;
import com.solarl.nado.entity.User;
import com.solarl.nado.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * единая платформа уведомлений.
 * все события проходят через send() → dedup → in-app persist → channel routing.
 * каналы: IN_APP (обязательный), EMAIL (extension point).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * единственная точка входа для всех уведомлений.
     * dedup: не создаёт дубль если уже есть идентичное (type + dedupKey).
     */
    @Transactional
    public void send(User recipient, NotificationType type, Map<String, Object> payloadMap) {
        String payload = serializePayload(payloadMap);
        String dedupKey = computeDedupKey(payloadMap);

        if (repository.existsByUserIdAndTypeAndDedupKey(recipient.getId(), type, dedupKey)) {
            log.debug("NOTIFICATION_DEDUP: userId={}, type={}", recipient.getId(), type);
            return;
        }

        Notification notification = Notification.builder()
                .user(recipient)
                .type(type)
                .payload(payload)
                .dedupKey(dedupKey)
                .build();
        repository.save(notification);

        log.info("NOTIFICATION_SENT: userId={}, type={}", recipient.getId(), type);

        routeToChannels(recipient, type, payloadMap);
    }



    public void notifyAdApproved(User owner, Long adId, String adTitle) {
        send(owner, NotificationType.AD_APPROVED, Map.of(
                "adId", adId, "adTitle", adTitle));
    }

    public void notifyAdRejected(User owner, Long adId, String adTitle, String reason) {
        send(owner, NotificationType.AD_REJECTED, Map.of(
                "adId", adId, "adTitle", adTitle, "reason", reason));
    }

    public void notifyAdBlocked(User owner, Long adId, String adTitle, String reason) {
        send(owner, NotificationType.AD_BLOCKED, Map.of(
                "adId", adId, "adTitle", adTitle, "reason", reason));
    }

    public void notifyAuctionOutbid(User previousLeader, Long auctionId, Long adId, String adTitle) {
        send(previousLeader, NotificationType.AUCTION_OUTBID, Map.of(
                "auctionId", auctionId, "adId", adId, "adTitle", adTitle));
    }

    public void notifyAuctionWon(User winner, Long auctionId, Long adId, String adTitle, String finalPrice) {
        send(winner, NotificationType.AUCTION_WON, Map.of(
                "auctionId", auctionId, "adId", adId, "adTitle", adTitle, "finalPrice", finalPrice));
    }

    public void notifyAuctionFinishedSeller(User seller, Long auctionId, Long adId, String adTitle, String finalPrice, String winnerName) {
        send(seller, NotificationType.AUCTION_FINISHED_SELLER, Map.of(
                "auctionId", auctionId, "adId", adId, "adTitle", adTitle,
                "finalPrice", finalPrice, "winnerName", winnerName));
    }

    public void notifyAuctionNoBids(User seller, Long auctionId, Long adId, String adTitle) {
        send(seller, NotificationType.AUCTION_NO_BIDS, Map.of(
                "auctionId", auctionId, "adId", adId, "adTitle", adTitle));
    }

    public void notifyWantedMatch(User buyer, Long requestId, Long adId, String adTitle) {
        send(buyer, NotificationType.WANTED_MATCH, Map.of(
                "requestId", requestId, "adId", adId, "adTitle", adTitle));
    }

    public void notifyNewMessage(User recipient, String senderName, Long adId, String adTitle) {
        send(recipient, NotificationType.NEW_MESSAGE, Map.of(
                "senderName", senderName, "adId", adId, "adTitle", adTitle));
    }

    public void notifyNewRating(User seller, String reviewerName, int score) {
        send(seller, NotificationType.NEW_RATING, Map.of(
                "reviewerName", reviewerName, "score", score));
    }

    // обратная совместимость: старые вызовы из CommentService и т.д.
    public void notifyNewComment(String email, String adTitle, String commenterName) {
        log.info("NOTIFICATION_LEGACY: comment on '{}' by {} → {}", adTitle, commenterName, email);
    }



    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(Long userId, int page, int size) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return repository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        repository.findById(notificationId).ifPresent(n -> {
            if (n.getUser().getId().equals(userId)) {
                n.setIsRead(true);
                repository.save(n);
            }
        });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unread = repository.findByUserIdAndIsReadFalse(userId);
        unread.forEach(n -> n.setIsRead(true));
        repository.saveAll(unread);
    }



    private void routeToChannels(User recipient, NotificationType type, Map<String, Object> payload) {
        // in-app уже персистирован; email — при наличии emailVerified

        // EMAIL — extension point: только если emailVerified
        // TODO: inject EmailService, check recipient.getEmailVerified()
        // if (recipient.getEmailVerified()) { emailService.send(...); }
    }



    private String serializePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.warn("не удалось сериализовать payload: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * Каноническая дедупликация: сортируем ключи → SHA-256.
     * Порядок полей в JSON больше не влияет на dedup.
     */
    private String computeDedupKey(Map<String, Object> payloadMap) {
        try {
            // TreeMap гарантирует каноничный порядок ключей
            String canonical = objectMapper.writeValueAsString(new TreeMap<>(payloadMap));
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            log.warn("не удалось вычислить dedupKey: {}", e.getMessage());
            return "";
        }
    }

    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType().name())
                .payload(n.getPayload())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
