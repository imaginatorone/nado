package com.solarl.nado.service;

import com.solarl.nado.entity.*;
import com.solarl.nado.repository.WantToBuyRepository;
import com.solarl.nado.repository.WantedMatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WantedMatchingServiceTest {

    @Mock private WantToBuyRepository wantedRepository;
    @Mock private WantedMatchRepository matchRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks private WantedMatchingService service;

    private User seller;
    private User buyer;
    private Category electronics;
    private Ad ad;

    @BeforeEach
    void setUp() {
        seller = User.builder().id(1L).name("Продавец").build();
        buyer = User.builder().id(2L).name("Покупатель").build();
        electronics = Category.builder().id(1L).name("Электроника").build();

        ad = Ad.builder()
                .id(10L).title("iPhone 15 Pro Max")
                .price(BigDecimal.valueOf(90000))
                .category(electronics)
                .region("Москва")
                .user(seller)
                .status(Ad.Status.PUBLISHED)
                .build();
    }

    @Test
    @DisplayName("Матч: совпадение по категории + цена + region + keywords")
    void matchPublishedAd_fullMatch() {
        WantToBuy request = WantToBuy.builder()
                .id(1L).user(buyer).query("iPhone")
                .category(electronics)
                .priceFrom(BigDecimal.valueOf(50000))
                .priceTo(BigDecimal.valueOf(100000))
                .region("Москва")
                .active(true).matchCount(0)
                .build();

        when(wantedRepository.findMatchingRequests(anyString(), anyLong(), any()))
                .thenReturn(List.of(request));
        when(matchRepository.existsByRequestIdAndAdId(1L, 10L)).thenReturn(false);

        service.matchPublishedAd(ad);

        verify(matchRepository).save(argThat(m ->
                m.getAd().getId().equals(10L)
                && m.getRequest().getId().equals(1L)
                && m.getScore() >= 25 // category(10) + price(10) + region(5) + keyword(5) = 30
        ));
        verify(wantedRepository).save(argThat(w -> w.getMatchCount() == 1));
    }

    @Test
    @DisplayName("Дедупликация: повторный матч не создаётся")
    void matchPublishedAd_dedup() {
        WantToBuy request = WantToBuy.builder()
                .id(1L).user(buyer).query("iPhone")
                .category(electronics).active(true).matchCount(1)
                .build();

        when(wantedRepository.findMatchingRequests(anyString(), anyLong(), any()))
                .thenReturn(List.of(request));
        // уже существует
        when(matchRepository.existsByRequestIdAndAdId(1L, 10L)).thenReturn(true);

        service.matchPublishedAd(ad);

        verify(matchRepository, never()).save(any());
    }

    @Test
    @DisplayName("Автор объявления не матчится со своим запросом")
    void matchPublishedAd_skipOwnAds() {
        WantToBuy ownRequest = WantToBuy.builder()
                .id(1L).user(seller).query("iPhone") // автор = seller = владелец объявления
                .category(electronics).active(true).matchCount(0)
                .build();

        when(wantedRepository.findMatchingRequests(anyString(), anyLong(), any()))
                .thenReturn(List.of(ownRequest));

        service.matchPublishedAd(ad);

        verify(matchRepository, never()).save(any());
    }

    @Test
    @DisplayName("Не-PUBLISHED объявление — пропускаем")
    void matchPublishedAd_skipNonPublished() {
        ad.setStatus(Ad.Status.DRAFT);

        service.matchPublishedAd(ad);

        verifyNoInteractions(wantedRepository);
        verifyNoInteractions(matchRepository);
    }

    @Test
    @DisplayName("Нет совпадений — ничего не сохраняется")
    void matchPublishedAd_noMatches() {
        when(wantedRepository.findMatchingRequests(anyString(), anyLong(), any()))
                .thenReturn(Collections.emptyList());

        service.matchPublishedAd(ad);

        verify(matchRepository, never()).save(any());
    }
}
