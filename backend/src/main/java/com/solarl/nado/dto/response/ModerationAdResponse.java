package com.solarl.nado.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// контекст модерации: содержит userEmail, rejectionReason, moderatedBy
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModerationAdResponse {
    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private String categoryName;
    private Long userId;
    private String userName;
    private String userEmail;
    private String status;
    private String rejectionReason;
    private Long moderatedById;
    private LocalDateTime submittedAt;
    private LocalDateTime moderatedAt;
    private LocalDateTime createdAt;
}
