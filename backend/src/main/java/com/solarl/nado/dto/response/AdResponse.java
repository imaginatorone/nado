package com.solarl.nado.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * публичное представление объявления.
 * не содержит userPhone и userEmail - контакты доступны только через защищённые
 * endpoint
 */
@Data
@Builder
public class AdResponse {
    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private Long categoryId;
    private String categoryName;
    private Long userId;
    private String userName;
    private String region;
    private String status;
    private String imageUrl;
    private List<ImageResponse> images;
    private int commentCount;
    private long viewCount;
    private long favoriteCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class ImageResponse {
        private Long id;
        private String url;
        private int sortOrder;
    }
}
