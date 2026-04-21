package com.solarl.nado.service;

import com.solarl.nado.entity.User;
import com.solarl.nado.dto.response.TrustRatingResponse;
import com.solarl.nado.exception.ResourceNotFoundException;
import com.solarl.nado.repository.RatingRepository;
import com.solarl.nado.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrustRatingServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RatingRepository ratingRepository;

    @InjectMocks private TrustRatingService service;

    private User createUser(boolean emailVerified, boolean phoneVerified, String region,
                            int deals, int complaints, int daysOld) {
        User u = new User();
        u.setId(1L);
        u.setName("Тест");
        u.setEmail("test@test.com");
        u.setEmailVerified(emailVerified);
        u.setPhoneVerified(phoneVerified);
        u.setRegion(region);
        u.setCompletedDeals(deals);
        u.setComplaints(complaints);
        u.setCreatedAt(LocalDateTime.now().minusDays(daysOld));
        return u;
    }

    @Test
    @DisplayName("Новый пользователь — низкий рейтинг")
    void newUser_lowTrust() {
        User user = createUser(false, false, null, 0, 0, 1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(ratingRepository.countBySellerId(1L)).thenReturn(0L);

        TrustRatingResponse r = service.calculateTrustRating(1L);

        assertThat(r.getTotalScore()).isLessThan(40);
        assertThat(r.getLevel()).isEqualTo("Низкий уровень доверия");
    }

    @Test
    @DisplayName("Проверенный пользователь — высокий рейтинг")
    void verifiedUser_highTrust() {
        User user = createUser(true, true, "Москва", 25, 0, 400);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(ratingRepository.countBySellerId(1L)).thenReturn(10L);
        when(ratingRepository.getAverageScoreBySellerId(1L)).thenReturn(4.8);

        TrustRatingResponse r = service.calculateTrustRating(1L);

        assertThat(r.getTotalScore()).isGreaterThanOrEqualTo(80);
        assertThat(r.getProfileScore()).isEqualTo(20);
        assertThat(r.getAgeScore()).isEqualTo(10);
        assertThat(r.getDealsScore()).isEqualTo(30);
    }

    @Test
    @DisplayName("Жалобы снижают рейтинг")
    void complaints_reduceTrust() {
        User user = createUser(true, true, "СПб", 10, 3, 200);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(ratingRepository.countBySellerId(1L)).thenReturn(5L);
        when(ratingRepository.getAverageScoreBySellerId(1L)).thenReturn(4.0);

        TrustRatingResponse r = service.calculateTrustRating(1L);

        assertThat(r.getComplaintsScore()).isLessThan(0);
        assertThat(r.getTotalScore()).isLessThan(80);
    }

    @Test
    @DisplayName("Пользователь не найден")
    void userNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.calculateTrustRating(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
