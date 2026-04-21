package com.solarl.nado.controller;

import com.solarl.nado.dto.response.AdResponse;
import com.solarl.nado.dto.response.PageResponse;
import com.solarl.nado.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    /**
     * Авторизованный — toggle избранное
     */
    @PostMapping("/{adId}")
    public ResponseEntity<Map<String, Object>> toggleFavorite(@PathVariable Long adId) {
        return ResponseEntity.ok(favoriteService.toggleFavorite(adId));
    }

    /**
     * Авторизованный — проверить, в избранном ли
     */
    @GetMapping("/{adId}/check")
    public ResponseEntity<Map<String, Object>> checkFavorite(@PathVariable Long adId) {
        boolean favorited = favoriteService.isFavorited(adId);
        long count = favoriteService.getFavoriteCount(adId);
        return ResponseEntity.ok(Map.of("favorited", favorited, "count", count));
    }

    /**
     * Авторизованный — мои избранные
     */
    @GetMapping
    public ResponseEntity<PageResponse<AdResponse>> getMyFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(favoriteService.getMyFavorites(page, size));
    }
}
