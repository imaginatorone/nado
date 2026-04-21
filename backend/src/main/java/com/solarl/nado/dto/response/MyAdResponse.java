package com.solarl.nado.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// кабинет владельца: видит свои статусы, rejectionReason, даты lifecycle
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyAdResponse {
    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private String categoryName;
    private Long categoryId;
    private String region;
    private String status;
    private String saleType;
    private String rejectionReason;
    private String imageUrl;
    private List<AdResponse.ImageResponse> images;
    private Long viewCount;
    private LocalDateTime submittedAt;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
