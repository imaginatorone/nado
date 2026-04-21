package com.solarl.nado.entity;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;

// дедупликация: запоминаем каждый матч (request_id, ad_id) ровно один раз
@Entity
@Table(name = "wanted_matches")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WantedMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private WantToBuy request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_id", nullable = false)
    private Ad ad;

    // релевантность: больше = лучше (category + price + region + keywords)
    @Column(nullable = false)
    @Builder.Default
    private Integer score = 0;

    // пользователь просмотрел этот матч
    @Column(nullable = false)
    @Builder.Default
    private Boolean seen = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
