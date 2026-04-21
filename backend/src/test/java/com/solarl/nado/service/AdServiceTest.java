package com.solarl.nado.service;

import com.solarl.nado.dto.request.AdCreateRequest;
import com.solarl.nado.dto.request.AdUpdateRequest;
import com.solarl.nado.dto.response.AdResponse;
import com.solarl.nado.dto.response.PageResponse;
import com.solarl.nado.entity.Ad;
import com.solarl.nado.entity.Category;
import com.solarl.nado.entity.User;
import com.solarl.nado.exception.ResourceNotFoundException;
import com.solarl.nado.repository.AdRepository;
import com.solarl.nado.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdServiceTest {

    @Mock
    private AdRepository adRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserService userService;
    @Mock
    private WantToBuyService wantToBuyService;
    @Mock
    private AdStatusTransitionService transitionService;

    @InjectMocks
    private AdService adService;

    private User testUser;
    private User otherUser;
    private Category testCategory;
    private Ad testAd;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L).name("Иван").email("ivan@test.com")
                .role(User.Role.USER).active(true).build();

        otherUser = User.builder()
                .id(2L).name("Пётр").email("petr@test.com")
                .role(User.Role.USER).active(true).build();

        testCategory = Category.builder()
                .id(1L).name("Транспорт").children(new ArrayList<>()).build();

        testAd = Ad.builder()
                .id(1L).title("Продам велосипед")
                .description("Горный, б/у").price(BigDecimal.valueOf(15000))
                .category(testCategory).user(testUser)
                .status(Ad.Status.PUBLISHED)
                .images(new ArrayList<>()).comments(new ArrayList<>())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Создание объявления → DRAFT + auto-submit")
    void createAd_success() {
        AdCreateRequest request = new AdCreateRequest();
        request.setTitle("Продам велосипед");
        request.setDescription("Горный, б/у");
        request.setPrice(BigDecimal.valueOf(15000));
        request.setCategoryId(1L);

        when(userService.getCurrentUserEntity()).thenReturn(testUser);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(adRepository.save(any(Ad.class))).thenAnswer(inv -> {
            Ad ad = inv.getArgument(0);
            ad.setId(1L);
            ad.setCreatedAt(LocalDateTime.now());
            ad.setUpdatedAt(LocalDateTime.now());
            return ad;
        });

        AdResponse response = adService.createAd(request);

        assertNotNull(response);
        assertEquals("Продам велосипед", response.getTitle());
        verify(adRepository).save(any(Ad.class));
        verify(transitionService).submitForModeration(1L);
    }

    @Test
    @DisplayName("Создание: несуществующая категория — ошибка")
    void createAd_categoryNotFound_throwsException() {
        AdCreateRequest request = new AdCreateRequest();
        request.setTitle("Тест");
        request.setDescription("Тест");
        request.setCategoryId(999L);

        when(userService.getCurrentUserEntity()).thenReturn(testUser);
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adService.createAd(request));
    }

    @Test
    @DisplayName("Получение по ID: найдено")
    void getAdById_found() {
        when(adRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(testAd));

        AdResponse response = adService.getAdById(1L);

        assertNotNull(response);
        assertEquals("Продам велосипед", response.getTitle());
    }

    @Test
    @DisplayName("Получение по ID: не найдено")
    void getAdById_notFound_throwsException() {
        when(adRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adService.getAdById(999L));
    }

    @Test
    @DisplayName("Обновление: автор + PUBLISHED → resubmit на модерацию")
    void updateAd_byOwner_success() {
        AdUpdateRequest request = new AdUpdateRequest();
        request.setTitle("Обновлённый заголовок");

        when(adRepository.findById(1L)).thenReturn(Optional.of(testAd));
        when(userService.getCurrentUserId()).thenReturn(1L);
        when(adRepository.save(any(Ad.class))).thenReturn(testAd);

        AdResponse response = adService.updateAd(1L, request);

        assertNotNull(response);
        verify(adRepository).save(any(Ad.class));
        // PUBLISHED → resubmit
        verify(transitionService).resubmitAfterEdit(1L);
    }

    @Test
    @DisplayName("Обновление: чужой пользователь — ошибка")
    void updateAd_byNonOwner_throwsException() {
        AdUpdateRequest request = new AdUpdateRequest();
        request.setTitle("Хак");

        when(adRepository.findById(1L)).thenReturn(Optional.of(testAd));
        when(userService.getCurrentUserId()).thenReturn(2L);

        assertThrows(IllegalArgumentException.class, () -> adService.updateAd(1L, request));
    }

    @Test
    @DisplayName("Удаление: делегируется в transition service")
    void deleteAd_delegatesToTransitionService() {
        adService.deleteAd(1L);

        verify(transitionService).removeAd(1L);
    }

    @Test
    @DisplayName("Публичная выдача: только PUBLISHED")
    void getAllActiveAds_returnsPaginatedResult() {
        Page<Ad> page = new PageImpl<>(List.of(testAd));
        when(adRepository.findAllPublished(any(Pageable.class))).thenReturn(page);

        PageResponse<AdResponse> response = adService.getAllActiveAds(0, 20);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    @Test
    @DisplayName("Мои объявления: owner query")
    void getMyAds_returnsCurrentUserAds() {
        when(userService.getCurrentUserId()).thenReturn(1L);
        Page<Ad> page = new PageImpl<>(List.of(testAd));
        when(adRepository.findByOwner(eq(1L), any(Pageable.class))).thenReturn(page);

        PageResponse<AdResponse> response = adService.getMyAds(0, 20);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }
}
