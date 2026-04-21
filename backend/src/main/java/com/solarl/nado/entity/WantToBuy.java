package com.solarl.nado.entity;

import lombok.*;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * «Хочу купить» — сохранённый поисковый запрос с автоматическим уведомлением
 * при появлении подходящего объявления.
 */
@Entity
@Table(name = "want_to_buy")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WantToBuy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Ключевая фраза / название товара */
    @Column(nullable = false, length = 255)
    private String query;

    /** Категория (опционально) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    /** Минимальная цена */
    @Column(name = "price_from", precision = 15, scale = 2)
    private BigDecimal priceFrom;

    /** Максимальная цена */
    @Column(name = "price_to", precision = 15, scale = 2)
    private BigDecimal priceTo;

    /** Регион / город */
    @Column(length = 100)
    private String region;

    /** Активен ли запрос */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /** Сколько раз сработало уведомление */
    @Column(name = "match_count", nullable = false)
    @Builder.Default
    private Integer matchCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_matched_at")
    private LocalDateTime lastMatchedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
