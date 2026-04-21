package com.solarl.nado.controller;

import com.solarl.nado.dto.request.AdCreateRequest;
import com.solarl.nado.dto.request.AdSearchRequest;
import com.solarl.nado.dto.request.AdUpdateRequest;
import com.solarl.nado.dto.response.AdResponse;
import com.solarl.nado.dto.response.MyAdResponse;
import com.solarl.nado.dto.response.PageResponse;
import com.solarl.nado.dto.response.PhoneResponse;
import com.solarl.nado.service.AdService;
import com.solarl.nado.service.AdStatusTransitionService;
import com.solarl.nado.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/ads")
@RequiredArgsConstructor
@Tag(name = "Объявления", description = "CRUD операции с объявлениями")
public class AdController {

    private final AdService adService;
    private final AdStatusTransitionService transitionService;
    private final UserService userService;

    @Operation(summary = "Список активных объявлений")
    @GetMapping
    public ResponseEntity<PageResponse<AdResponse>> getAllAds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adService.getAllActiveAds(page, size));
    }

    @Operation(summary = "Поиск объявлений")
    @GetMapping("/search")
    public ResponseEntity<PageResponse<AdResponse>> searchAds(AdSearchRequest request) {
        return ResponseEntity.ok(adService.searchAds(request));
    }

    @Operation(summary = "Карточка объявления")
    @GetMapping("/{id}")
    public ResponseEntity<AdResponse> getAd(@PathVariable Long id) {
        return ResponseEntity.ok(adService.getAdById(id));
    }

    @Operation(summary = "Создать объявление")
    @PostMapping
    public ResponseEntity<AdResponse> createAd(@Valid @RequestBody AdCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adService.createAd(request));
    }

    @Operation(summary = "Редактировать объявление")
    @PutMapping("/{id}")
    public ResponseEntity<AdResponse> updateAd(
            @PathVariable Long id,
            @Valid @RequestBody AdUpdateRequest request) {
        return ResponseEntity.ok(adService.updateAd(id, request));
    }

    @Operation(summary = "Удалить объявление")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAd(@PathVariable Long id) {
        adService.deleteAd(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Мои объявления")
    @GetMapping("/my")
    public ResponseEntity<PageResponse<AdResponse>> getMyAds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adService.getMyAds(page, size));
    }

    // кабинет владельца — с lifecycle-данными и фильтром по статусу
    @Operation(summary = "Мои объявления (кабинет)")
    @GetMapping("/my/cabinet")
    public ResponseEntity<PageResponse<MyAdResponse>> getMyCabinet(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(adService.getMyAdsTyped(page, size, status));
    }

    // раскрытие контакта — требует auth (см. SecurityConfig)
    @Operation(summary = "Телефон продавца")
    @GetMapping("/{id}/phone")
    public ResponseEntity<PhoneResponse> getSellerPhone(@PathVariable Long id) {
        Long userId = userService.getCurrentUserId();
        String phone = adService.getSellerPhone(id);
        log.info("AUDIT: user id={} запросил телефон по объявлению id={}", userId, id);
        return ResponseEntity.ok(userService.revealPhone(phone));
    }

    @Operation(summary = "Пометить как продано")
    @PatchMapping("/{id}/sold")
    public ResponseEntity<Void> markSold(@PathVariable Long id) {
        transitionService.markSold(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Снять с публикации / архивировать")
    @PatchMapping("/{id}/archive")
    public ResponseEntity<Void> archiveAd(@PathVariable Long id) {
        transitionService.archiveAd(id);
        return ResponseEntity.ok().build();
    }
}
