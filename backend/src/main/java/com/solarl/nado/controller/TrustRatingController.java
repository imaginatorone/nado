package com.solarl.nado.controller;

import com.solarl.nado.dto.response.TrustRatingResponse;
import com.solarl.nado.service.TrustRatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/trust")
@RequiredArgsConstructor
public class TrustRatingController {

    private final TrustRatingService trustRatingService;

    /**
     * Публичный — рейтинг доверия пользователя
     */
    @GetMapping("/{userId}")
    public ResponseEntity<TrustRatingResponse> getTrustRating(@PathVariable Long userId) {
        return ResponseEntity.ok(trustRatingService.calculateTrustRating(userId));
    }
}
