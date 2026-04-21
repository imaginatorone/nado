package com.solarl.nado.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Публичная информация о пользователе — для просмотра другими пользователями.
 * НЕ содержит email и phone.
 */
@Data
@Builder
public class UserPublicResponse {
    private Long id;
    private String name;
    private String role;
    private String region;
    private String avatarUrl;
    private Integer completedDeals;

    // статистика
    private Long adsCount;
    private Long reviewsCount;

    private LocalDateTime createdAt;
}
