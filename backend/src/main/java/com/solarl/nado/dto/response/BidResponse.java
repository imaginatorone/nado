package com.solarl.nado.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BidResponse {
    private Long id;
    private Long bidderId;
    private String bidderName;
    private BigDecimal amount;
    private LocalDateTime createdAt;
}
