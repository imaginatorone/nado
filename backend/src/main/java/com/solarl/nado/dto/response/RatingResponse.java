package com.solarl.nado.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class RatingResponse {
    private Long id;
    private Long reviewerId;
    private String reviewerName;
    private Long sellerId;
    private String sellerName;
    private Integer score;
    private String review;
    private LocalDateTime createdAt;
}
