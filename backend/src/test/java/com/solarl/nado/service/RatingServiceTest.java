package com.solarl.nado.service;

import com.solarl.nado.dto.request.RatingCreateRequest;
import com.solarl.nado.dto.response.RatingResponse;
import com.solarl.nado.entity.Rating;
import com.solarl.nado.entity.User;
import com.solarl.nado.exception.ResourceNotFoundException;
import com.solarl.nado.repository.AdRepository;
import com.solarl.nado.repository.RatingRepository;
import com.solarl.nado.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock private RatingRepository ratingRepository;
    @Mock private UserRepository userRepository;
    @Mock private AdRepository adRepository;
    @Mock private UserService userService;

    @InjectMocks private RatingService ratingService;

    private User reviewer;
    private User seller;

    @BeforeEach
    void setUp() {
        reviewer = new User();
        reviewer.setId(1L);
        reviewer.setName("Покупатель");
        reviewer.setEmail("buyer@test.com");

        seller = new User();
        seller.setId(2L);
        seller.setName("Продавец");
        seller.setEmail("seller@test.com");
    }

    @Test
    @DisplayName("Создание рейтинга — успех")
    void createRating_success() {
        RatingCreateRequest request = new RatingCreateRequest();
        request.setScore(5);
        request.setReview("Отличный продавец!");

        when(userService.getCurrentUserEntity()).thenReturn(reviewer);
        when(userRepository.findById(2L)).thenReturn(Optional.of(seller));
        when(ratingRepository.findByReviewerIdAndSellerId(1L, 2L)).thenReturn(Optional.empty());
        when(ratingRepository.save(any(Rating.class))).thenAnswer(inv -> {
            Rating r = inv.getArgument(0);
            r.setId(1L);
            r.setCreatedAt(LocalDateTime.now());
            return r;
        });

        RatingResponse response = ratingService.createRating(2L, request);

        assertThat(response.getScore()).isEqualTo(5);
        assertThat(response.getReview()).isEqualTo("Отличный продавец!");
        assertThat(response.getReviewerName()).isEqualTo("Покупатель");
        verify(ratingRepository).save(any(Rating.class));
    }

    @Test
    @DisplayName("Нельзя оценить самого себя")
    void createRating_selfRating_throws() {
        when(userService.getCurrentUserEntity()).thenReturn(reviewer);
        RatingCreateRequest request = new RatingCreateRequest();
        request.setScore(5);

        assertThatThrownBy(() -> ratingService.createRating(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("самого себя");
    }

    @Test
    @DisplayName("Нельзя оценить дважды")
    void createRating_duplicate_throws() {
        RatingCreateRequest request = new RatingCreateRequest();
        request.setScore(4);

        when(userService.getCurrentUserEntity()).thenReturn(reviewer);
        when(userRepository.findById(2L)).thenReturn(Optional.of(seller));
        when(ratingRepository.findByReviewerIdAndSellerId(1L, 2L))
                .thenReturn(Optional.of(new Rating()));

        assertThatThrownBy(() -> ratingService.createRating(2L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("уже оценили");
    }

    @Test
    @DisplayName("Продавец не найден")
    void createRating_sellerNotFound_throws() {
        RatingCreateRequest request = new RatingCreateRequest();
        request.setScore(3);

        when(userService.getCurrentUserEntity()).thenReturn(reviewer);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ratingService.createRating(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Получение рейтингов продавца")
    void getSellerRatings_returnsList() {
        Rating r = Rating.builder()
                .id(1L).reviewer(reviewer).seller(seller)
                .score(5).review("Супер").createdAt(LocalDateTime.now())
                .build();

        when(ratingRepository.findBySellerIdOrderByCreatedAtDesc(2L)).thenReturn(List.of(r));

        List<RatingResponse> result = ratingService.getSellerRatings(2L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getScore()).isEqualTo(5);
    }
}
