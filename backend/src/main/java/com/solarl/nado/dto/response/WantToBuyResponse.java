package com.solarl.nado.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class WantToBuyResponse {
    private Long id;
    private String query;
    private Long categoryId;
    private String categoryName;
    private BigDecimal priceFrom;
    private BigDecimal priceTo;
    private String region;
    private Boolean active;
    private Integer matchCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastMatchedAt;
}
