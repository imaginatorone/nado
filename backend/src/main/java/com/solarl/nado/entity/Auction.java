package com.solarl.nado.entity;

import lombok.*;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "auctions")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_id", nullable = false, unique = true)
    private Ad ad;

    @Column(name = "start_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal startPrice;

    @Column(name = "current_price", precision = 15, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "final_price", precision = 15, scale = 2)
    private BigDecimal finalPrice;

    @Column(name = "min_step", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal minStep = BigDecimal.valueOf(100);

    // продление при ставке в последние N минут
    @Column(name = "bid_extension_minutes", nullable = false)
    @Builder.Default
    private Integer bidExtensionMinutes = 5;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Column(name = "last_bid_at")
    private LocalDateTime lastBidAt;

    @Column(name = "bid_count", nullable = false)
    @Builder.Default
    private Integer bidCount = 0;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AuctionStatus status = AuctionStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private User winner;

    @OneToMany(mappedBy = "auction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt DESC")
    @Builder.Default
    private List<Bid> bids = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (currentPrice == null) {
            currentPrice = startPrice;
        }
    }

    /**
     * ACTIVE → FINISHED (есть ставки) / NO_BIDS (нет ставок)
     * NO_BIDS → owner решает (продлить / отменить / fixed-price)
     * любой (без ставок) → CANCELLED
     * пост-аукционная сделка — в AuctionOutcome, не здесь
     */
    public enum AuctionStatus {
        ACTIVE,
        FINISHED,
        NO_BIDS,
        CANCELLED
    }
}
