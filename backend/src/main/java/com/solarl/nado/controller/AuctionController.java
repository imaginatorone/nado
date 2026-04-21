package com.solarl.nado.controller;

import com.solarl.nado.dto.request.AuctionCreateRequest;
import com.solarl.nado.dto.request.BidRequest;
import com.solarl.nado.dto.response.AuctionResponse;
import com.solarl.nado.dto.response.BidResponse;
import com.solarl.nado.service.AuctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;

    @PostMapping
    public ResponseEntity<AuctionResponse> createAuction(
            @Valid @RequestBody AuctionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(auctionService.createAuction(request));
    }

    @GetMapping("/ad/{adId}")
    public ResponseEntity<AuctionResponse> getAuctionByAdId(@PathVariable Long adId) {
        return ResponseEntity.ok(auctionService.getAuctionByAdId(adId));
    }

    @GetMapping("/active")
    public ResponseEntity<List<AuctionResponse>> getActiveAuctions() {
        return ResponseEntity.ok(auctionService.getActiveAuctions());
    }

    @PostMapping("/{auctionId}/bid")
    public ResponseEntity<BidResponse> placeBid(
            @PathVariable Long auctionId,
            @Valid @RequestBody BidRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(auctionService.placeBid(auctionId, request));
    }

    @PostMapping("/{auctionId}/cancel")
    public ResponseEntity<AuctionResponse> cancelAuction(@PathVariable Long auctionId) {
        return ResponseEntity.ok(auctionService.cancelAuction(auctionId));
    }

    @PostMapping("/{auctionId}/extend")
    public ResponseEntity<AuctionResponse> extendAuction(
            @PathVariable Long auctionId,
            @RequestBody Map<String, String> body) {
        LocalDateTime newEndsAt = LocalDateTime.parse(body.get("endsAt"));
        return ResponseEntity.ok(auctionService.extendAuction(auctionId, newEndsAt));
    }
}
