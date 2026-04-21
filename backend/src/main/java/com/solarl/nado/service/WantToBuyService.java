package com.solarl.nado.service;

import com.solarl.nado.dto.request.WantToBuyRequest;
import com.solarl.nado.dto.response.WantToBuyResponse;
import com.solarl.nado.dto.response.WantedMatchResponse;
import com.solarl.nado.entity.Category;
import com.solarl.nado.entity.WantToBuy;
import com.solarl.nado.entity.WantedMatch;
import com.solarl.nado.exception.ResourceNotFoundException;
import com.solarl.nado.repository.CategoryRepository;
import com.solarl.nado.repository.WantToBuyRepository;
import com.solarl.nado.repository.WantedMatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WantToBuyService {

    private final WantToBuyRepository repository;
    private final WantedMatchRepository matchRepository;
    private final CategoryRepository categoryRepository;
    private final UserService userService;

    @Transactional
    public WantToBuyResponse create(WantToBuyRequest request) {
        var user = userService.getCurrentUserEntity();
        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("категория не найдена"));
        }

        var wtb = WantToBuy.builder()
                .user(user)
                .query(request.getQuery())
                .category(category)
                .priceFrom(request.getPriceFrom())
                .priceTo(request.getPriceTo())
                .region(request.getRegion())
                .build();

        wtb = repository.save(wtb);
        log.info("WANTED_CREATED: userId={}, query='{}'", user.getId(), request.getQuery());
        return mapToResponse(wtb);
    }

    @Transactional(readOnly = true)
    public List<WantToBuyResponse> getMyRequests() {
        Long userId = userService.getCurrentUserId();
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deactivate(Long id) {
        var wtb = findOwnedRequest(id);
        wtb.setActive(false);
        repository.save(wtb);
    }

    @Transactional
    public void delete(Long id) {
        var wtb = findOwnedRequest(id);
        repository.delete(wtb);
    }

    // матчи для конкретного запроса
    @Transactional(readOnly = true)
    public List<WantedMatchResponse> getMatches(Long requestId) {
        var wtb = findOwnedRequest(requestId);
        return matchRepository.findByRequestIdOrderByScoreDescCreatedAtDesc(requestId).stream()
                .map(this::mapMatchToResponse)
                .collect(Collectors.toList());
    }

    // пометить матчи как просмотренные
    @Transactional
    public void markMatchesSeen(Long requestId) {
        var wtb = findOwnedRequest(requestId);
        List<WantedMatch> unseen = matchRepository
                .findByRequestIdOrderByScoreDescCreatedAtDesc(requestId)
                .stream().filter(m -> !m.getSeen()).collect(Collectors.toList());
        unseen.forEach(m -> m.setSeen(true));
        matchRepository.saveAll(unseen);
    }

    // общее кол-во непросмотренных матчей пользователя
    @Transactional(readOnly = true)
    public long getUnseenCount() {
        Long userId = userService.getCurrentUserId();
        return matchRepository.countByRequestUserIdAndSeenFalse(userId);
    }

    private WantToBuy findOwnedRequest(Long id) {
        var wtb = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("запрос не найден"));
        Long userId = userService.getCurrentUserId();
        if (!wtb.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("нет прав");
        }
        return wtb;
    }

    private WantToBuyResponse mapToResponse(WantToBuy wtb) {
        return WantToBuyResponse.builder()
                .id(wtb.getId())
                .query(wtb.getQuery())
                .categoryId(wtb.getCategory() != null ? wtb.getCategory().getId() : null)
                .categoryName(wtb.getCategory() != null ? wtb.getCategory().getName() : null)
                .priceFrom(wtb.getPriceFrom())
                .priceTo(wtb.getPriceTo())
                .region(wtb.getRegion())
                .active(wtb.getActive())
                .matchCount(wtb.getMatchCount())
                .createdAt(wtb.getCreatedAt())
                .lastMatchedAt(wtb.getLastMatchedAt())
                .build();
    }

    private WantedMatchResponse mapMatchToResponse(WantedMatch match) {
        var ad = match.getAd();
        return WantedMatchResponse.builder()
                .matchId(match.getId())
                .adId(ad.getId())
                .adTitle(ad.getTitle())
                .adPrice(ad.getPrice())
                .adRegion(ad.getRegion())
                .adSaleType(ad.getSaleType() != null ? ad.getSaleType().name() : null)
                .categoryName(ad.getCategory() != null ? ad.getCategory().getName() : null)
                .score(match.getScore())
                .seen(match.getSeen())
                .matchedAt(match.getCreatedAt())
                .build();
    }
}
