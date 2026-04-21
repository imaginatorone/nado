package com.solarl.nado.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * публичный профиль продавца.
 * не содержит email и phone - приватные данные пользователя.
 */
@Data
@Builder
public class SellerProfileResponse {
    private Long id;
    private String name;
    private String avatarUrl;
    private double averageRating;
    private long ratingCount;
    private long adCount;
    private String memberSince;
    private List<RatingResponse> recentRatings;
}
