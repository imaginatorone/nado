package com.solarl.nado.service;

import com.solarl.nado.dto.response.AdminDashboardResponse;
import com.solarl.nado.dto.response.AdminDashboardResponse.*;
import com.solarl.nado.entity.Ad;
import com.solarl.nado.entity.Auction;
import com.solarl.nado.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsService {

    private final UserRepository userRepository;
    private final AdRepository adRepository;
    private final AuctionRepository auctionRepository;
    private final WantToBuyRepository wantToBuyRepository;
    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        return AdminDashboardResponse.builder()
                .users(buildUserStats())
                .ads(buildAdStats())
                .auctions(buildAuctionStats())
                .wantedTotal(wantToBuyRepository.count())
                .notificationsTotal(notificationRepository.count())
                .pendingModeration(adRepository.countByStatus(Ad.Status.PENDING_MODERATION))
                .topCategories(buildTopCategories())
                .dailyAds(buildDailyAds())
                .build();
    }

    private UserStats buildUserStats() {
        return UserStats.builder()
                .total(userRepository.count())
                .active(userRepository.countByActiveTrue())
                .phoneVerified(userRepository.countByPhoneVerifiedTrue())
                .emailVerified(userRepository.countByEmailVerifiedTrue())
                .banned(userRepository.countByBannedTrue())
                .build();
    }

    private AdStats buildAdStats() {
        // одним запросом вместо N count()
        Map<String, Long> statusMap = adRepository.countByStatusGrouped().stream()
                .collect(Collectors.toMap(
                        row -> row[0].toString(),
                        row -> (Long) row[1]
                ));

        return AdStats.builder()
                .total(adRepository.count())
                .published(statusMap.getOrDefault("PUBLISHED", 0L))
                .pending(statusMap.getOrDefault("PENDING_MODERATION", 0L))
                .rejected(statusMap.getOrDefault("REJECTED", 0L))
                .sold(statusMap.getOrDefault("SOLD", 0L))
                .archived(statusMap.getOrDefault("ARCHIVED", 0L))
                .blocked(statusMap.getOrDefault("BLOCKED", 0L))
                .build();
    }

    private AuctionStats buildAuctionStats() {
        return AuctionStats.builder()
                .total(auctionRepository.count())
                .active(auctionRepository.countByStatus(Auction.AuctionStatus.ACTIVE))
                .finished(auctionRepository.countByStatus(Auction.AuctionStatus.FINISHED))
                .noBids(auctionRepository.countByStatus(Auction.AuctionStatus.NO_BIDS))
                .cancelled(auctionRepository.countByStatus(Auction.AuctionStatus.CANCELLED))
                .build();
    }

    private List<CategoryStat> buildTopCategories() {
        return adRepository.topCategories().stream()
                .limit(10)
                .map(row -> CategoryStat.builder()
                        .name((String) row[0])
                        .count((Long) row[1])
                        .build())
                .collect(Collectors.toList());
    }

    // динамика за последние 30 дней
    private List<DailyStat> buildDailyAds() {
        return adRepository.dailyAdCounts(LocalDateTime.now().minusDays(30)).stream()
                .map(row -> DailyStat.builder()
                        .date(row[0].toString())
                        .count((Long) row[1])
                        .build())
                .collect(Collectors.toList());
    }
}
