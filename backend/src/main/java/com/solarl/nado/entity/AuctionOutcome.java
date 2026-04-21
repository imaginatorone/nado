package com.solarl.nado.entity;

import lombok.*;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// пост-аукционная сделка: связывает победителя и продавца
@Entity
@Table(name = "auction_outcomes")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuctionOutcome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false, unique = true)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(name = "final_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal finalPrice;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OutcomeStatus status = OutcomeStatus.PENDING_CONTACT;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * PENDING_CONTACT → CONTACTED → COMPLETED / FAILED
     * победитель и продавец подтверждают завершение сделки
     */
    public enum OutcomeStatus {
        PENDING_CONTACT,
        CONTACTED,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}
