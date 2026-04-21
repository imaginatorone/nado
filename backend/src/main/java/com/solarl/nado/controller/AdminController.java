package com.solarl.nado.controller;

import com.solarl.nado.dto.response.AdResponse;
import com.solarl.nado.dto.response.AdminStatsResponse;
import com.solarl.nado.dto.response.PageResponse;
import com.solarl.nado.dto.response.ToggleActiveResponse;
import com.solarl.nado.dto.response.UserPrivateResponse;
import com.solarl.nado.service.AdService;
import com.solarl.nado.service.ModerationService;
import com.solarl.nado.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdService adService;
    private final UserService userService;
    private final ModerationService moderationService;

    @GetMapping("/ads")
    public ResponseEntity<PageResponse<AdResponse>> getAllAds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adService.getAllAdsForAdmin(page, size));
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserPrivateResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsersForAdmin());
    }

    // делегируем в AdStatusTransitionService через AdService.deleteAd
    @DeleteMapping("/ads/{id}")
    public ResponseEntity<Void> deleteAd(@PathVariable Long id) {
        adService.deleteAd(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/{id}/toggle-active")
    public ResponseEntity<ToggleActiveResponse> toggleUserActive(@PathVariable Long id) {
        return ResponseEntity.ok(userService.toggleUserActiveAndRespond(id));
    }

    @PostMapping("/users/{id}/ban")
    public ResponseEntity<Void> banUser(@PathVariable Long id) {
        userService.banUser(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(AdminStatsResponse.builder()
                .totalUsers(userService.countAll())
                .totalAds(adService.countAll())
                .pendingModeration(moderationService.getPendingCount())
                .build());
    }
}
