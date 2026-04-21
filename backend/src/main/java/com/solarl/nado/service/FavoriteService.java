package com.solarl.nado.service;

import com.solarl.nado.dto.response.AdResponse;
import com.solarl.nado.dto.response.PageResponse;
import com.solarl.nado.entity.Ad;
import com.solarl.nado.entity.Favorite;
import com.solarl.nado.entity.User;
import com.solarl.nado.exception.ResourceNotFoundException;
import com.solarl.nado.repository.AdRepository;
import com.solarl.nado.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final AdRepository adRepository;
    private final UserService userService;
    private final AdService adService;

    @Transactional
    public Map<String, Object> toggleFavorite(Long adId) {
        Long userId = userService.getCurrentUserId();
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> new ResourceNotFoundException("Объявление не найдено"));

        var existing = favoriteRepository.findByUserIdAndAdId(userId, adId);
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            log.info("Удалено из избранного: userId={}, adId={}", userId, adId);
            return Map.of("favorited", false, "count", favoriteRepository.countByAdId(adId));
        } else {
            User user = userService.getCurrentUserEntity();
            Favorite fav = Favorite.builder().user(user).ad(ad).build();
            favoriteRepository.save(fav);
            log.info("Добавлено в избранное: userId={}, adId={}", userId, adId);
            return Map.of("favorited", true, "count", favoriteRepository.countByAdId(adId));
        }
    }

    @Transactional(readOnly = true)
    public boolean isFavorited(Long adId) {
        Long userId = userService.getCurrentUserId();
        return favoriteRepository.existsByUserIdAndAdId(userId, adId);
    }

    @Transactional(readOnly = true)
    public long getFavoriteCount(Long adId) {
        return favoriteRepository.countByAdId(adId);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdResponse> getMyFavorites(int page, int size) {
        Long userId = userService.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<Favorite> favPage = favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<AdResponse> content = favPage.getContent().stream()
                .map(fav -> adService.mapAdToResponse(fav.getAd()))
                .collect(Collectors.toList());

        return PageResponse.<AdResponse>builder()
                .content(content)
                .page(favPage.getNumber())
                .size(favPage.getSize())
                .totalElements(favPage.getTotalElements())
                .totalPages(favPage.getTotalPages())
                .last(favPage.isLast())
                .build();
    }
}
