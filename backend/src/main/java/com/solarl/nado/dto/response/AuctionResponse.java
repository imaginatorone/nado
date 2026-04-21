package com.solarl.nado.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AuctionResponse {
    private Long id;
    private Long adId;
    private String adTitle;
    private BigDecimal startPrice;
    private BigDecimal currentPrice;
    private BigDecimal finalPrice;
    private BigDecimal minStep;
    private Integer bidExtensionMinutes;
    private LocalDateTime endsAt;
    private LocalDateTime lastBidAt;
    private String status;
    private int bidCount;
    private Long winnerId;
    private String winnerName;
    private List<BidResponse> recentBids;
    private LocalDateTime createdAt;
}
