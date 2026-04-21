package com.solarl.nado.controller;

import com.solarl.nado.dto.request.ModerationBlockRequest;
import com.solarl.nado.dto.request.ModerationRejectRequest;
import com.solarl.nado.dto.response.ModerationAdResponse;
import com.solarl.nado.dto.response.PageResponse;
import com.solarl.nado.service.ModerationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/moderation")
@RequiredArgsConstructor
public class ModerationController {

    private final ModerationService moderationService;

    @Operation(summary = "Очередь на модерацию")
    @GetMapping("/pending")
    public ResponseEntity<PageResponse<ModerationAdResponse>> getPending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(moderationService.getPendingAds(page, size));
    }

    @Operation(summary = "Одобрить объявление")
    @PostMapping("/{adId}/approve")
    public ResponseEntity<ModerationAdResponse> approve(@PathVariable Long adId) {
        return ResponseEntity.ok(moderationService.approve(adId));
    }

    @Operation(summary = "Отклонить объявление")
    @PostMapping("/{adId}/reject")
    public ResponseEntity<ModerationAdResponse> reject(
            @PathVariable Long adId,
            @Valid @RequestBody ModerationRejectRequest request) {
        return ResponseEntity.ok(moderationService.reject(adId, request.getReason()));
    }

    @Operation(summary = "Заблокировать объявление")
    @PostMapping("/{adId}/block")
    public ResponseEntity<ModerationAdResponse> block(
            @PathVariable Long adId,
            @Valid @RequestBody ModerationBlockRequest request) {
        return ResponseEntity.ok(moderationService.block(adId, request.getReason()));
    }

    @Operation(summary = "Количество ожидающих модерации")
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getPendingCount() {
        return ResponseEntity.ok(Map.of("count", moderationService.getPendingCount()));
    }
}
