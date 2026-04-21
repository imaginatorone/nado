package com.solarl.nado.service;

import com.solarl.nado.dto.request.RatingCreateRequest;
import com.solarl.nado.dto.response.RatingResponse;
import com.solarl.nado.dto.response.SellerProfileResponse;
import com.solarl.nado.entity.Rating;
import com.solarl.nado.entity.User;
import com.solarl.nado.exception.ResourceNotFoundException;
import com.solarl.nado.repository.AdRepository;
import com.solarl.nado.repository.RatingRepository;
import com.solarl.nado.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final AdRepository adRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<RatingResponse> getSellerRatings(Long sellerId) {
        return ratingRepository.findBySellerIdOrderByCreatedAtDesc(sellerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Публичный профиль продавца — НЕ содержит email и phone.
     */
    @Transactional(readOnly = true)
    public SellerProfileResponse getSellerProfile(Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Продавец не найден"));

        double avgRating = ratingRepository.getAverageScoreBySellerId(sellerId);
        long ratingCount = ratingRepository.countBySellerId(sellerId);
        long adCount = adRepository.countByUserId(sellerId);

        List<RatingResponse> recentRatings = ratingRepository.findBySellerIdOrderByCreatedAtDesc(sellerId)
                .stream()
                .limit(5)
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return SellerProfileResponse.builder()
                .id(seller.getId())
                .name(seller.getName())
                .avatarUrl(seller.getAvatarUrl())
                // email и phone убраны — приватные данные
                .averageRating(Math.round(avgRating * 10.0) / 10.0)
                .ratingCount(ratingCount)
                .adCount(adCount)
                .memberSince(seller.getCreatedAt() != null
                        ? seller.getCreatedAt().format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
                        : "")
                .recentRatings(recentRatings)
                .build();
    }

    @Transactional
    public RatingResponse createRating(Long sellerId, RatingCreateRequest request) {
        User reviewer = userService.getCurrentUserEntity();

        if (reviewer.getId().equals(sellerId)) {
            throw new IllegalArgumentException("Нельзя оценить самого себя");
        }

        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Продавец не найден"));

        ratingRepository.findByReviewerIdAndSellerId(reviewer.getId(), sellerId)
                .ifPresent(r -> {
                    throw new IllegalArgumentException("Вы уже оценили этого продавца");
                });

        Rating rating = Rating.builder()
                .reviewer(reviewer)
                .seller(seller)
                .score(request.getScore())
                .review(request.getReview())
                .build();

        rating = ratingRepository.save(rating);
        log.info("Создан рейтинг: reviewer={}, seller={}, score={}",
                reviewer.getId(), sellerId, request.getScore());
        return mapToResponse(rating);
    }

    private RatingResponse mapToResponse(Rating rating) {
        return RatingResponse.builder()
                .id(rating.getId())
                .reviewerId(rating.getReviewer().getId())
                .reviewerName(rating.getReviewer().getName())
                .sellerId(rating.getSeller().getId())
                .sellerName(rating.getSeller().getName())
                .score(rating.getScore())
                .review(rating.getReview())
                .createdAt(rating.getCreatedAt())
                .build();
    }
}
