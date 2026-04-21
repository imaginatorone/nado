package com.solarl.nado.entity;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    // JSON-строка с контекстом: adId, title, reason, amount и т.д.
    @Column(nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String payload = "{}";

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * единый реестр типов уведомлений.
     * тип определяет смысл, payload — данные, routing policy — каналы.
     */
    public enum NotificationType {
        // модерация
        AD_APPROVED,
        AD_REJECTED,
        AD_BLOCKED,

        // аукционы
        AUCTION_OUTBID,
        AUCTION_WON,
        AUCTION_FINISHED_SELLER,
        AUCTION_NO_BIDS,

        // wanted
        WANTED_MATCH,

        // чат
        NEW_MESSAGE,

        // рейтинг
        NEW_RATING,

        // общее
        SYSTEM
    }
}
