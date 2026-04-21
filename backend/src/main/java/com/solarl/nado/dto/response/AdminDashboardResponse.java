package com.solarl.nado.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * полная аналитика платформы для admin dashboard.
 * каждая секция - отдельный typed блок, не Map.
 */
@Data
@Builder
public class AdminDashboardResponse {

    private UserStats users;
    private AdStats ads;
    private AuctionStats auctions;
    private long wantedTotal;
    private long notificationsTotal;
    private long pendingModeration;
    private List<CategoryStat> topCategories;
    private List<DailyStat> dailyAds;

    @Data
    @Builder
    public static class UserStats {
        private long total;
        private long active;
        private long phoneVerified;
        private long emailVerified;
        private long banned;
    }

    @Data
    @Builder
    public static class AdStats {
        private long total;
        private long published;
        private long pending;
        private long rejected;
        private long sold;
        private long archived;
        private long blocked;
    }

    @Data
    @Builder
    public static class AuctionStats {
        private long total;
        private long active;
        private long finished;
        private long noBids;
        private long cancelled;
    }

    @Data
    @Builder
    public static class CategoryStat {
        private String name;
        private long count;
    }

    @Data
    @Builder
    public static class DailyStat {
        private String date;
        private long count;
    }
}
