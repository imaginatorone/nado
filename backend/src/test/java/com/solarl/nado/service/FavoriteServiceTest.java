package com.solarl.nado.service;

import com.solarl.nado.entity.Ad;
import com.solarl.nado.entity.Favorite;
import com.solarl.nado.entity.User;
import com.solarl.nado.exception.ResourceNotFoundException;
import com.solarl.nado.repository.AdRepository;
import com.solarl.nado.repository.FavoriteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock private FavoriteRepository favoriteRepository;
    @Mock private AdRepository adRepository;
    @Mock private UserService userService;
    @Mock private AdService adService;

    @InjectMocks private FavoriteService favoriteService;

    private User user;
    private Ad ad;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("Тестовый");

        ad = Ad.builder().id(1L).title("Тест").build();
    }

    @Test
    @DisplayName("Добавить в избранное")
    void toggleFavorite_add() {
        when(userService.getCurrentUserId()).thenReturn(1L);
        when(userService.getCurrentUserEntity()).thenReturn(user);
        when(adRepository.findById(1L)).thenReturn(Optional.of(ad));
        when(favoriteRepository.findByUserIdAndAdId(1L, 1L)).thenReturn(Optional.empty());
        when(favoriteRepository.countByAdId(1L)).thenReturn(1L);

        Map<String, Object> result = favoriteService.toggleFavorite(1L);

        assertThat(result.get("favorited")).isEqualTo(true);
        verify(favoriteRepository).save(any(Favorite.class));
    }

    @Test
    @DisplayName("Убрать из избранного")
    void toggleFavorite_remove() {
        Favorite existing = Favorite.builder().id(1L).user(user).ad(ad).build();

        when(userService.getCurrentUserId()).thenReturn(1L);
        when(adRepository.findById(1L)).thenReturn(Optional.of(ad));
        when(favoriteRepository.findByUserIdAndAdId(1L, 1L)).thenReturn(Optional.of(existing));
        when(favoriteRepository.countByAdId(1L)).thenReturn(0L);

        Map<String, Object> result = favoriteService.toggleFavorite(1L);

        assertThat(result.get("favorited")).isEqualTo(false);
        verify(favoriteRepository).delete(existing);
    }

    @Test
    @DisplayName("Объявление не найдено")
    void toggleFavorite_adNotFound() {
        when(userService.getCurrentUserId()).thenReturn(1L);
        when(adRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.toggleFavorite(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Проверка: в избранном")
    void isFavorited_true() {
        when(userService.getCurrentUserId()).thenReturn(1L);
        when(favoriteRepository.existsByUserIdAndAdId(1L, 1L)).thenReturn(true);

        assertThat(favoriteService.isFavorited(1L)).isTrue();
    }
}
