package com.solarl.nado.service;

import com.solarl.nado.dto.response.TrustRatingResponse;
import com.solarl.nado.entity.User;
import com.solarl.nado.exception.ResourceNotFoundException;
import com.solarl.nado.repository.RatingRepository;
import com.solarl.nado.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Система «Рейтинг доверия» — 100-балльная шкала надёжности пользователя.
 *
 * Факторы:
 * - Подтверждённость профиля: до 20 баллов
 * - Возраст аккаунта: до 10 баллов
 * - Завершённые сделки: до 30 баллов
 * - Отзывы: от -20 до +30 баллов
 * - Жалобы: от 0 до -30 баллов
 */
@Service
@RequiredArgsConstructor
public class TrustRatingService {

    private final UserRepository userRepository;
    private final RatingRepository ratingRepository;

    @Transactional(readOnly = true)
    public TrustRatingResponse calculateTrustRating(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        int profileScore = calculateProfileScore(user);
        int ageScore = calculateAgeScore(user);
        int dealsScore = calculateDealsScore(user);
        int reviewsScore = calculateReviewsScore(userId);
        int complaintsScore = calculateComplaintsScore(user);

        int total = profileScore + ageScore + dealsScore + reviewsScore + complaintsScore;
        total = Math.max(0, Math.min(100, total));

        String level = determineTrustLevel(total);

        return TrustRatingResponse.builder()
                .userId(userId)
                .userName(user.getName())
                .totalScore(total)
                .level(level)
                .profileScore(profileScore)
                .ageScore(ageScore)
                .dealsScore(dealsScore)
                .reviewsScore(reviewsScore)
                .complaintsScore(complaintsScore)
                .emailVerified(user.getEmailVerified())
                .phoneVerified(user.getPhoneVerified())
                .region(user.getRegion())
                .completedDeals(user.getCompletedDeals())
                .complaints(user.getComplaints())
                .memberSince(user.getCreatedAt())
                .build();
    }

    /** Подтверждённость профиля: до 20 баллов */
    private int calculateProfileScore(User user) {
        int score = 0;
        if (Boolean.TRUE.equals(user.getEmailVerified())) score += 5;
        if (Boolean.TRUE.equals(user.getPhoneVerified())) score += 10;
        if (user.getName() != null && !user.getName().isBlank() &&
            user.getRegion() != null && !user.getRegion().isBlank()) {
            score += 5;
        }
        return score;
    }

    /** Возраст аккаунта: до 10 баллов */
    private int calculateAgeScore(User user) {
        if (user.getCreatedAt() == null) return 0;
        long days = ChronoUnit.DAYS.between(user.getCreatedAt(), LocalDateTime.now());
        if (days > 365) return 10;
        if (days > 180) return 8;
        if (days > 90) return 6;
        if (days > 30) return 4;
        if (days > 7) return 2;
        return 0;
    }

    /** Завершённые сделки: до 30 баллов */
    private int calculateDealsScore(User user) {
        int deals = user.getCompletedDeals() != null ? user.getCompletedDeals() : 0;
        if (deals > 20) return 30;
        if (deals > 10) return 24;
        if (deals > 5) return 18;
        if (deals > 2) return 10;
        if (deals >= 1) return 5;
        return 0;
    }

    /** Отзывы: от -20 до +30 баллов (на основе среднего балла и количества) */
    private int calculateReviewsScore(Long userId) {
        long count = ratingRepository.countBySellerId(userId);
        if (count == 0) return 0;

        double avg = ratingRepository.getAverageScoreBySellerId(userId);

        // Каждый отзыв даёт от -5 до +3 баллов в зависимости от оценки
        // avg >= 4.5 → +3 за отзыв, avg 3.5-4.5 → +1, avg < 2.5 → -5
        int perReview;
        if (avg >= 4.5) perReview = 3;
        else if (avg >= 3.5) perReview = 1;
        else if (avg >= 2.5) perReview = 0;
        else perReview = -5;

        int score = (int)(perReview * Math.min(count, 10));
        return Math.max(-20, Math.min(30, score));
    }

    /** Жалобы: от 0 до -30 баллов */
    private int calculateComplaintsScore(User user) {
        int complaints = user.getComplaints() != null ? user.getComplaints() : 0;
        if (complaints == 0) return 0;
        if (complaints == 1) return -5;
        if (complaints == 2) return -10;
        if (complaints == 3) return -15;
        if (complaints >= 4) return -20 - Math.min(10, (complaints - 3) * 5);
        return 0;
    }

    private String determineTrustLevel(int score) {
        if (score >= 90) return "Высокий уровень доверия";
        if (score >= 80) return "Проверенный пользователь";
        if (score >= 60) return "Надёжный пользователь";
        if (score >= 40) return "Базовый уровень доверия";
        return "Низкий уровень доверия";
    }
}
