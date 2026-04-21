package com.solarl.nado.service;

import com.solarl.nado.dto.request.AuctionCreateRequest;
import com.solarl.nado.dto.request.BidRequest;
import com.solarl.nado.dto.response.AuctionResponse;
import com.solarl.nado.dto.response.BidResponse;
import com.solarl.nado.entity.*;
import com.solarl.nado.repository.AdRepository;
import com.solarl.nado.repository.AuctionOutcomeRepository;
import com.solarl.nado.repository.AuctionRepository;
import com.solarl.nado.repository.BidRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

    @Mock private AuctionRepository auctionRepository;
    @Mock private AuctionOutcomeRepository outcomeRepository;
    @Mock private BidRepository bidRepository;
    @Mock private AdRepository adRepository;
    @Mock private UserService userService;
    @Mock private NotificationService notificationService;

    @InjectMocks private AuctionService auctionService;

    private User owner;
    private User bidder;
    private Ad ad;
    private Category category;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setName("Продавец");

        bidder = new User();
        bidder.setId(2L);
        bidder.setName("Покупатель");

        category = Category.builder().id(1L).name("Электроника").auctionAllowed(true).build();

        // объявление должно быть PUBLISHED + AUCTION для создания аукциона
        ad = Ad.builder()
                .id(1L).title("Товар").user(owner)
                .status(Ad.Status.PUBLISHED)
                .saleType(Ad.SaleType.AUCTION)
                .category(category)
                .build();
    }

    @Test
    @DisplayName("Создание аукциона — успех")
    void createAuction_success() {
        AuctionCreateRequest request = new AuctionCreateRequest();
        request.setAdId(1L);
        request.setStartPrice(BigDecimal.valueOf(1000));
        request.setMinStep(BigDecimal.valueOf(100));
        request.setEndsAt(LocalDateTime.now().plusDays(1));

        when(userService.getCurrentUserEntity()).thenReturn(owner);
        when(adRepository.findById(1L)).thenReturn(Optional.of(ad));
        when(auctionRepository.findByAdId(1L)).thenReturn(Optional.empty());
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> {
            Auction a = inv.getArgument(0);
            a.setId(1L);
            a.setCreatedAt(LocalDateTime.now());
            return a;
        });
        when(bidRepository.findByAuctionIdOrderByAmountDesc(anyLong())).thenReturn(Collections.emptyList());

        AuctionResponse response = auctionService.createAuction(request);

        assertThat(response.getStartPrice()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        verify(auctionRepository).save(any(Auction.class));
    }

    @Test
    @DisplayName("Нельзя создать аукцион для чужого объявления")
    void createAuction_notOwner_throws() {
        AuctionCreateRequest request = new AuctionCreateRequest();
        request.setAdId(1L);
        request.setStartPrice(BigDecimal.valueOf(500));
        request.setEndsAt(LocalDateTime.now().plusDays(1));

        when(userService.getCurrentUserEntity()).thenReturn(bidder);
        when(adRepository.findById(1L)).thenReturn(Optional.of(ad));

        assertThatThrownBy(() -> auctionService.createAuction(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("своего");
    }

    @Test
    @DisplayName("Нельзя создать аукцион для неопубликованного объявления")
    void createAuction_draftAd_throws() {
        Ad draftAd = Ad.builder().id(2L).title("Черновик").user(owner)
                .status(Ad.Status.DRAFT).saleType(Ad.SaleType.AUCTION).build();

        AuctionCreateRequest request = new AuctionCreateRequest();
        request.setAdId(2L);
        request.setStartPrice(BigDecimal.valueOf(500));
        request.setEndsAt(LocalDateTime.now().plusDays(1));

        when(userService.getCurrentUserEntity()).thenReturn(owner);
        when(adRepository.findById(2L)).thenReturn(Optional.of(draftAd));

        assertThatThrownBy(() -> auctionService.createAuction(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("опубликовано");
    }

    @Test
    @DisplayName("Нельзя создать аукцион в запрещённой категории")
    void createAuction_categoryForbidden_throws() {
        Category noAuctions = Category.builder().id(2L).name("Услуги").auctionAllowed(false).build();
        Ad noAuctionAd = Ad.builder().id(3L).title("Услуга").user(owner)
                .status(Ad.Status.PUBLISHED).saleType(Ad.SaleType.AUCTION)
                .category(noAuctions).build();

        AuctionCreateRequest request = new AuctionCreateRequest();
        request.setAdId(3L);
        request.setStartPrice(BigDecimal.valueOf(500));
        request.setEndsAt(LocalDateTime.now().plusDays(1));

        when(userService.getCurrentUserEntity()).thenReturn(owner);
        when(adRepository.findById(3L)).thenReturn(Optional.of(noAuctionAd));

        assertThatThrownBy(() -> auctionService.createAuction(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("запрещены");
    }

    @Test
    @DisplayName("Ставка — успех + продление при ставке в последние минуты")
    void placeBid_success_withExtension() {
        Auction auction = Auction.builder()
                .id(1L).ad(ad)
                .startPrice(BigDecimal.valueOf(1000))
                .currentPrice(BigDecimal.valueOf(1000))
                .minStep(BigDecimal.valueOf(100))
                .bidExtensionMinutes(5)
                .bidCount(0)
                .status(Auction.AuctionStatus.ACTIVE)
                .endsAt(LocalDateTime.now().plusMinutes(3)) // менее 5 мин → будет продление
                .build();

        BidRequest request = new BidRequest();
        request.setAmount(BigDecimal.valueOf(1100));

        when(userService.getCurrentUserEntity()).thenReturn(bidder);
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction));
        when(bidRepository.save(any(Bid.class))).thenAnswer(inv -> {
            Bid b = inv.getArgument(0);
            b.setId(1L);
            b.setCreatedAt(LocalDateTime.now());
            return b;
        });

        BidResponse response = auctionService.placeBid(1L, request);

        assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1100));
        assertThat(auction.getCurrentPrice()).isEqualByComparingTo(BigDecimal.valueOf(1100));
        assertThat(auction.getBidCount()).isEqualTo(1);
        // аукцион продлён — endsAt сдвинулся вперёд
        assertThat(auction.getEndsAt()).isAfter(LocalDateTime.now().plusMinutes(4));
    }

    @Test
    @DisplayName("Ставка ниже минимума — ошибка")
    void placeBid_tooLow_throws() {
        Auction auction = Auction.builder()
                .id(1L).ad(ad)
                .currentPrice(BigDecimal.valueOf(1000))
                .minStep(BigDecimal.valueOf(100))
                .bidExtensionMinutes(5)
                .bidCount(0)
                .status(Auction.AuctionStatus.ACTIVE)
                .endsAt(LocalDateTime.now().plusHours(2))
                .build();

        BidRequest request = new BidRequest();
        request.setAmount(BigDecimal.valueOf(1050));

        when(userService.getCurrentUserEntity()).thenReturn(bidder);
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction));

        assertThatThrownBy(() -> auctionService.placeBid(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("минимальная ставка");
    }

    @Test
    @DisplayName("Нельзя ставить на свой аукцион")
    void placeBid_ownAuction_throws() {
        Auction auction = Auction.builder()
                .id(1L).ad(ad)
                .currentPrice(BigDecimal.valueOf(1000))
                .minStep(BigDecimal.valueOf(100))
                .bidExtensionMinutes(5)
                .bidCount(0)
                .status(Auction.AuctionStatus.ACTIVE)
                .endsAt(LocalDateTime.now().plusHours(2))
                .build();

        BidRequest request = new BidRequest();
        request.setAmount(BigDecimal.valueOf(1100));

        when(userService.getCurrentUserEntity()).thenReturn(owner);
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction));

        assertThatThrownBy(() -> auctionService.placeBid(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("свой");
    }

    @Test
    @DisplayName("Отмена аукциона без ставок — успех")
    void cancelAuction_noBids_success() {
        Auction auction = Auction.builder()
                .id(1L).ad(ad)
                .bidCount(0)
                .status(Auction.AuctionStatus.ACTIVE)
                .endsAt(LocalDateTime.now().plusHours(2))
                .build();

        when(userService.getCurrentUserEntity()).thenReturn(owner);
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction));
        when(auctionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bidRepository.findByAuctionIdOrderByAmountDesc(anyLong())).thenReturn(Collections.emptyList());

        AuctionResponse response = auctionService.cancelAuction(1L);
        assertThat(response.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("Нельзя отменить аукцион со ставками")
    void cancelAuction_withBids_throws() {
        Auction auction = Auction.builder()
                .id(1L).ad(ad)
                .bidCount(3)
                .status(Auction.AuctionStatus.ACTIVE)
                .build();

        when(userService.getCurrentUserEntity()).thenReturn(owner);
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction));

        assertThatThrownBy(() -> auctionService.cancelAuction(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ставками");
    }
}
