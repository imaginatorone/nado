package com.solarl.nado.service;

import com.solarl.nado.dto.request.AuctionCreateRequest;
import com.solarl.nado.dto.request.BidRequest;
import com.solarl.nado.dto.response.AuctionResponse;
import com.solarl.nado.dto.response.BidResponse;
import com.solarl.nado.entity.*;
import com.solarl.nado.exception.ResourceNotFoundException;
import com.solarl.nado.repository.AdRepository;
import com.solarl.nado.repository.AuctionOutcomeRepository;
import com.solarl.nado.repository.AuctionRepository;
import com.solarl.nado.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final AuctionOutcomeRepository outcomeRepository;
    private final BidRepository bidRepository;
    private final AdRepository adRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    @Transactional
    public AuctionResponse createAuction(AuctionCreateRequest request) {
        User user = userService.getCurrentUserEntity();
        Ad ad = adRepository.findById(request.getAdId())
                .orElseThrow(() -> new ResourceNotFoundException("объявление не найдено"));

        if (!ad.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("аукцион можно создать только для своего объявления");
        }

        // объявление должно быть опубликовано с типом AUCTION
        if (ad.getStatus() != Ad.Status.PUBLISHED) {
            throw new IllegalArgumentException("объявление должно быть опубликовано");
        }

        if (ad.getSaleType() != Ad.SaleType.AUCTION) {
            throw new IllegalArgumentException("тип продажи должен быть AUCTION");
        }

        // категория разрешает аукционы
        if (ad.getCategory() != null && !ad.getCategory().getAuctionAllowed()) {
            throw new IllegalArgumentException("аукционы запрещены для этой категории");
        }

        if (auctionRepository.findByAdId(request.getAdId()).isPresent()) {
            throw new IllegalArgumentException("аукцион для этого объявления уже существует");
        }

        if (request.getEndsAt().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new IllegalArgumentException("аукцион должен длиться минимум 1 час");
        }

        Auction auction = Auction.builder()
                .ad(ad)
                .startPrice(request.getStartPrice())
                .currentPrice(request.getStartPrice())
                .minStep(request.getMinStep() != null ? request.getMinStep() : BigDecimal.valueOf(100))
                .bidExtensionMinutes(request.getBidExtensionMinutes() != null ? request.getBidExtensionMinutes() : 5)
                .endsAt(request.getEndsAt())
                .build();

        auction = auctionRepository.save(auction);
        log.info("AUCTION_CREATED: id={}, adId={}, startPrice={}, endsAt={}",
                auction.getId(), ad.getId(), request.getStartPrice(), request.getEndsAt());
        return mapToResponse(auction);
    }

    @Transactional(readOnly = true)
    public AuctionResponse getAuctionByAdId(Long adId) {
        Auction auction = auctionRepository.findByAdId(adId)
                .orElseThrow(() -> new ResourceNotFoundException("аукцион не найден"));
        return mapToResponse(auction);
    }

    @Transactional(readOnly = true)
    public List<AuctionResponse> getActiveAuctions() {
        return auctionRepository.findActiveAuctions().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public BidResponse placeBid(Long auctionId, BidRequest request) {
        User bidder = userService.getCurrentUserEntity();
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("аукцион не найден"));

        if (auction.getStatus() != Auction.AuctionStatus.ACTIVE) {
            throw new IllegalArgumentException("аукцион не активен");
        }

        if (auction.getEndsAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("время аукциона истекло");
        }

        if (auction.getAd().getUser().getId().equals(bidder.getId())) {
            throw new IllegalArgumentException("нельзя делать ставку на свой аукцион");
        }

        BigDecimal minBid = auction.getCurrentPrice().add(auction.getMinStep());
        if (request.getAmount().compareTo(minBid) < 0) {
            throw new IllegalArgumentException("минимальная ставка: " + minBid);
        }

        // предыдущий лидер — для notification hook
        User previousLeader = auction.getWinner();

        Bid bid = Bid.builder()
                .auction(auction)
                .bidder(bidder)
                .amount(request.getAmount())
                .build();

        bid = bidRepository.save(bid);
        auction.setCurrentPrice(request.getAmount());
        auction.setWinner(bidder);
        auction.setLastBidAt(LocalDateTime.now());
        auction.setBidCount(auction.getBidCount() + 1);

        // продление при ставке в последние N минут
        LocalDateTime extensionThreshold = auction.getEndsAt()
                .minusMinutes(auction.getBidExtensionMinutes());
        if (LocalDateTime.now().isAfter(extensionThreshold)) {
            LocalDateTime newEnd = LocalDateTime.now().plusMinutes(auction.getBidExtensionMinutes());
            auction.setEndsAt(newEnd);
            log.info("AUCTION_EXTENDED: id={}, newEndsAt={}", auction.getId(), newEnd);
        }

        auctionRepository.save(auction);

        // --- notification hooks (заготовки для Phase 5) ---
        onBidPlaced(auction, bid, previousLeader);

        log.info("BID_PLACED: auctionId={}, bidder={}, amount={}, bidCount={}",
                auctionId, bidder.getId(), request.getAmount(), auction.getBidCount());
        return mapBidToResponse(bid);
    }

    // scheduled: завершение аукционов каждые 30 секунд
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void finalizeExpiredAuctions() {
        List<Auction> expired = auctionRepository.findExpiredAuctions(LocalDateTime.now());
        for (Auction auction : expired) {
            if (auction.getBidCount() > 0 && auction.getWinner() != null) {
                // есть ставки → FINISHED, создаём outcome
                auction.setStatus(Auction.AuctionStatus.FINISHED);
                auction.setFinalPrice(auction.getCurrentPrice());
                auctionRepository.save(auction);

                createOutcome(auction);
                onAuctionFinished(auction);

                log.info("AUCTION_FINISHED: id={}, winner={}, finalPrice={}",
                        auction.getId(), auction.getWinner().getId(), auction.getFinalPrice());
            } else {
                // нет ставок → NO_BIDS
                auction.setStatus(Auction.AuctionStatus.NO_BIDS);
                auctionRepository.save(auction);

                onAuctionNoBids(auction);
                log.info("AUCTION_NO_BIDS: id={}", auction.getId());
            }
        }
    }

    // owner: отмена аукциона (только если нет ставок)
    @Transactional
    public AuctionResponse cancelAuction(Long auctionId) {
        User user = userService.getCurrentUserEntity();
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("аукцион не найден"));

        if (!auction.getAd().getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("только владелец может отменить аукцион");
        }

        if (auction.getBidCount() > 0) {
            throw new IllegalArgumentException("нельзя отменить аукцион со ставками");
        }

        if (auction.getStatus() != Auction.AuctionStatus.ACTIVE
                && auction.getStatus() != Auction.AuctionStatus.NO_BIDS) {
            throw new IllegalStateException("аукцион нельзя отменить в текущем статусе");
        }

        auction.setStatus(Auction.AuctionStatus.CANCELLED);
        auctionRepository.save(auction);
        log.info("AUCTION_CANCELLED: id={}, by={}", auctionId, user.getId());
        return mapToResponse(auction);
    }

    // owner: продление аукциона без ставок
    @Transactional
    public AuctionResponse extendAuction(Long auctionId, LocalDateTime newEndsAt) {
        User user = userService.getCurrentUserEntity();
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("аукцион не найден"));

        if (!auction.getAd().getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("только владелец может продлить аукцион");
        }

        if (auction.getStatus() != Auction.AuctionStatus.NO_BIDS
                && auction.getStatus() != Auction.AuctionStatus.ACTIVE) {
            throw new IllegalStateException("продление доступно только для активного аукциона или без ставок");
        }

        if (newEndsAt.isBefore(LocalDateTime.now().plusHours(1))) {
            throw new IllegalArgumentException("продление минимум на 1 час");
        }

        auction.setEndsAt(newEndsAt);
        if (auction.getStatus() == Auction.AuctionStatus.NO_BIDS) {
            auction.setStatus(Auction.AuctionStatus.ACTIVE);
        }
        auctionRepository.save(auction);
        log.info("AUCTION_EXTENDED_BY_OWNER: id={}, newEndsAt={}", auctionId, newEndsAt);
        return mapToResponse(auction);
    }

    // --- notification hooks ---

    private void onBidPlaced(Auction auction, Bid bid, User previousLeader) {
        if (previousLeader != null && !previousLeader.getId().equals(bid.getBidder().getId())) {
            notificationService.notifyAuctionOutbid(
                    previousLeader, auction.getId(), auction.getAd().getId(), auction.getAd().getTitle());
        }
    }

    private void onAuctionFinished(Auction auction) {
        String price = auction.getFinalPrice().toPlainString();
        notificationService.notifyAuctionWon(
                auction.getWinner(), auction.getId(), auction.getAd().getId(),
                auction.getAd().getTitle(), price);
        notificationService.notifyAuctionFinishedSeller(
                auction.getAd().getUser(), auction.getId(), auction.getAd().getId(),
                auction.getAd().getTitle(), price, auction.getWinner().getName());
    }

    private void onAuctionNoBids(Auction auction) {
        notificationService.notifyAuctionNoBids(
                auction.getAd().getUser(), auction.getId(), auction.getAd().getId(),
                auction.getAd().getTitle());
    }

    // --- helpers ---

    private void createOutcome(Auction auction) {
        AuctionOutcome outcome = AuctionOutcome.builder()
                .auction(auction)
                .buyer(auction.getWinner())
                .seller(auction.getAd().getUser())
                .finalPrice(auction.getFinalPrice())
                .build();
        outcomeRepository.save(outcome);
    }

    private AuctionResponse mapToResponse(Auction auction) {
        List<BidResponse> recentBids = bidRepository
                .findByAuctionIdOrderByAmountDesc(auction.getId())
                .stream()
                .limit(10)
                .map(this::mapBidToResponse)
                .collect(Collectors.toList());

        return AuctionResponse.builder()
                .id(auction.getId())
                .adId(auction.getAd().getId())
                .adTitle(auction.getAd().getTitle())
                .startPrice(auction.getStartPrice())
                .currentPrice(auction.getCurrentPrice())
                .finalPrice(auction.getFinalPrice())
                .minStep(auction.getMinStep())
                .bidExtensionMinutes(auction.getBidExtensionMinutes())
                .endsAt(auction.getEndsAt())
                .lastBidAt(auction.getLastBidAt())
                .status(auction.getStatus().name())
                .bidCount(auction.getBidCount())
                .winnerId(auction.getWinner() != null ? auction.getWinner().getId() : null)
                .winnerName(auction.getWinner() != null ? auction.getWinner().getName() : null)
                .recentBids(recentBids)
                .createdAt(auction.getCreatedAt())
                .build();
    }

    private BidResponse mapBidToResponse(Bid bid) {
        return BidResponse.builder()
                .id(bid.getId())
                .bidderId(bid.getBidder().getId())
                .bidderName(bid.getBidder().getName())
                .amount(bid.getAmount())
                .createdAt(bid.getCreatedAt())
                .build();
    }
}
