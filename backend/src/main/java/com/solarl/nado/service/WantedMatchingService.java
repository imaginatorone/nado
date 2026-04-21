package com.solarl.nado.service;

import com.solarl.nado.entity.Ad;
import com.solarl.nado.entity.WantToBuy;
import com.solarl.nado.entity.WantedMatch;
import com.solarl.nado.repository.WantToBuyRepository;
import com.solarl.nado.repository.WantedMatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * rule-based matching engine:
 * при публикации объявления проверяет все активные wanted-запросы,
 * считает score и сохраняет матч с дедупликацией.
 *
 * матчинг только по PUBLISHED объявлениям — вызывается из AdStatusTransitionService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WantedMatchingService {

    private final WantToBuyRepository wantedRepository;
    private final WantedMatchRepository matchRepository;
    private final NotificationService notificationService;

    // вызывается при переходе объявления в PUBLISHED
    @Transactional
    public void matchPublishedAd(Ad ad) {
        if (ad.getStatus() != Ad.Status.PUBLISHED) return;

        List<WantToBuy> candidates = wantedRepository.findMatchingRequests(
                ad.getTitle(),
                ad.getCategory() != null ? ad.getCategory().getId() : null,
                ad.getPrice());

        int created = 0;
        for (WantToBuy request : candidates) {
            // не матчим автора объявления
            if (request.getUser().getId().equals(ad.getUser().getId())) continue;

            // дедупликация — один матч на пару (request, ad)
            if (matchRepository.existsByRequestIdAndAdId(request.getId(), ad.getId())) continue;

            int score = calculateScore(request, ad);

            WantedMatch match = WantedMatch.builder()
                    .request(request)
                    .ad(ad)
                    .score(score)
                    .build();
            matchRepository.save(match);

            request.setMatchCount(request.getMatchCount() + 1);
            request.setLastMatchedAt(LocalDateTime.now());
            wantedRepository.save(request);

            created++;
            notificationService.notifyWantedMatch(
                    request.getUser(), request.getId(), ad.getId(), ad.getTitle());
        }

        if (created > 0) {
            log.info("WANTED_MATCH: adId={}, created {} matches", ad.getId(), created);
        }
    }

    /**
     * rule-based scoring:
     * +10 category match
     * +10 price in range
     * +5  region match
     * +5  keywords overlap
     */
    private int calculateScore(WantToBuy request, Ad ad) {
        int score = 0;

        // категория
        if (request.getCategory() != null && ad.getCategory() != null
                && request.getCategory().getId().equals(ad.getCategory().getId())) {
            score += 10;
        }

        // цена в диапазоне
        if (ad.getPrice() != null) {
            boolean fromOk = request.getPriceFrom() == null
                    || ad.getPrice().compareTo(request.getPriceFrom()) >= 0;
            boolean toOk = request.getPriceTo() == null
                    || ad.getPrice().compareTo(request.getPriceTo()) <= 0;
            if (fromOk && toOk) score += 10;
        }

        // регион
        if (request.getRegion() != null && ad.getRegion() != null
                && request.getRegion().equalsIgnoreCase(ad.getRegion())) {
            score += 5;
        }

        // keywords — проверяем overlap query words с title
        if (request.getQuery() != null && ad.getTitle() != null) {
            String[] queryWords = request.getQuery().toLowerCase().split("\\s+");
            String titleLower = ad.getTitle().toLowerCase();
            for (String word : queryWords) {
                if (word.length() >= 3 && titleLower.contains(word)) {
                    score += 5;
                    break; // один бонус за keyword overlap
                }
            }
        }

        return score;
    }
}
