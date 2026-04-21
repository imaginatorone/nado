package com.solarl.nado.service;

import com.solarl.nado.dto.response.ModerationAdResponse;
import com.solarl.nado.dto.response.PageResponse;
import com.solarl.nado.entity.Ad;
import com.solarl.nado.repository.AdRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModerationService {

    private final AdRepository adRepository;
    private final AdStatusTransitionService transitionService;

    @Transactional(readOnly = true)
    public PageResponse<ModerationAdResponse> getPendingAds(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Ad> adPage = adRepository.findPendingModeration(pageable);
        return toPageResponse(adPage);
    }

    @Transactional
    public ModerationAdResponse approve(Long adId) {
        Ad ad = transitionService.approve(adId);
        return mapToModerationResponse(ad);
    }

    @Transactional
    public ModerationAdResponse reject(Long adId, String reason) {
        Ad ad = transitionService.reject(adId, reason);
        return mapToModerationResponse(ad);
    }

    @Transactional
    public ModerationAdResponse block(Long adId, String reason) {
        Ad ad = transitionService.blockAd(adId, reason);
        return mapToModerationResponse(ad);
    }

    @Transactional(readOnly = true)
    public long getPendingCount() {
        return adRepository.countByStatus(Ad.Status.PENDING_MODERATION);
    }

    private ModerationAdResponse mapToModerationResponse(Ad ad) {
        return ModerationAdResponse.builder()
                .id(ad.getId())
                .title(ad.getTitle())
                .description(ad.getDescription())
                .price(ad.getPrice())
                .categoryName(ad.getCategory() != null ? ad.getCategory().getName() : null)
                .userId(ad.getUser().getId())
                .userName(ad.getUser().getName())
                .userEmail(ad.getUser().getEmail())
                .status(ad.getStatus().name())
                .rejectionReason(ad.getRejectionReason())
                .moderatedById(ad.getModeratedBy() != null ? ad.getModeratedBy().getId() : null)
                .submittedAt(ad.getSubmittedAt())
                .moderatedAt(ad.getModeratedAt())
                .createdAt(ad.getCreatedAt())
                .build();
    }

    private PageResponse<ModerationAdResponse> toPageResponse(Page<Ad> adPage) {
        List<ModerationAdResponse> content = adPage.getContent().stream()
                .map(this::mapToModerationResponse)
                .collect(Collectors.toList());

        return PageResponse.<ModerationAdResponse>builder()
                .content(content)
                .page(adPage.getNumber())
                .size(adPage.getSize())
                .totalElements(adPage.getTotalElements())
                .totalPages(adPage.getTotalPages())
                .last(adPage.isLast())
                .build();
    }
}
