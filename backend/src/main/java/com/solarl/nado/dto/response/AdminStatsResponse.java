package com.solarl.nado.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminStatsResponse {
    private long totalUsers;
    private long totalAds;
    private long pendingModeration;
}
