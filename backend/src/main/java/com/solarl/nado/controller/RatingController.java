package com.solarl.nado.controller;

import com.solarl.nado.dto.request.RatingCreateRequest;
import com.solarl.nado.dto.response.RatingResponse;
import com.solarl.nado.dto.response.SellerProfileResponse;
import com.solarl.nado.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    /**
     * Все — рейтинги продавца
     */
    @GetMapping("/ratings/seller/{sellerId}")
    public ResponseEntity<List<RatingResponse>> getSellerRatings(@PathVariable Long sellerId) {
        return ResponseEntity.ok(ratingService.getSellerRatings(sellerId));
    }

    /**
     * Все — профиль продавца с рейтингами
     */
    @GetMapping("/ratings/seller/{sellerId}/profile")
    public ResponseEntity<SellerProfileResponse> getSellerProfile(@PathVariable Long sellerId) {
        return ResponseEntity.ok(ratingService.getSellerProfile(sellerId));
    }

    /**
     * Авторизованный — оставить оценку
     */
    @PostMapping("/ratings/seller/{sellerId}")
    public ResponseEntity<RatingResponse> createRating(
            @PathVariable Long sellerId,
            @Valid @RequestBody RatingCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ratingService.createRating(sellerId, request));
    }
}
