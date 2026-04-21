package com.solarl.nado.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Полная информация о пользователе — только для владельца (/users/me) и администраторов.
 * Содержит email и phone.
 */
@Data
@Builder
public class UserPrivateResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String role;
    private Boolean active;

    // поля рейтинга доверия
    private Boolean phoneVerified;
    private Boolean emailVerified;
    private String region;
    private String avatarUrl;
    private Integer completedDeals;
    private Integer complaints;

    // статистика
    private Long adsCount;
    private Long reviewsCount;

    private LocalDateTime createdAt;
}
