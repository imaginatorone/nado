package com.solarl.nado.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class WantedMatchResponse {
    private Long matchId;
    private Long adId;
    private String adTitle;
    private BigDecimal adPrice;
    private String adRegion;
    private String adSaleType;
    private String categoryName;
    private int score;
    private boolean seen;
    private LocalDateTime matchedAt;
}
