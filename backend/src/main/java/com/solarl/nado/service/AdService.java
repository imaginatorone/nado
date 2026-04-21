package com.solarl.nado.service;

import com.solarl.nado.dto.request.AdCreateRequest;
import com.solarl.nado.dto.request.AdSearchRequest;
import com.solarl.nado.dto.request.AdUpdateRequest;
import com.solarl.nado.dto.response.AdResponse;
import com.solarl.nado.dto.response.MyAdResponse;
import com.solarl.nado.dto.response.PageResponse;
import com.solarl.nado.entity.Ad;
import com.solarl.nado.entity.Category;
import com.solarl.nado.entity.User;
import com.solarl.nado.exception.ResourceNotFoundException;
import com.solarl.nado.repository.AdRepository;
import com.solarl.nado.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdService {

    private final AdRepository adRepository;
    private final CategoryRepository categoryRepository;
    private final UserService userService;
    private final WantToBuyService wantToBuyService;
    private final AdStatusTransitionService transitionService;

    @Transactional(readOnly = true)
    public PageResponse<AdResponse> getAllActiveAds(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Ad> adPage = adRepository.findAllPublished(pageable);
        return toPageResponse(adPage);
    }

    @Transactional
    public AdResponse getAdById(Long id) {
        Ad ad = adRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Объявление не найдено"));
        ad.setViewCount(ad.getViewCount() + 1);
        adRepository.save(ad);
        return mapAdToResponse(ad);
    }


    @Transactional(readOnly = true)
    public String getSellerPhone(Long adId) {
        Ad ad = adRepository.findByIdWithDetails(adId)
                .orElseThrow(() -> new ResourceNotFoundException("Объявление не найдено"));
        return ad.getUser().getPhone();
    }

    @Transactional
    public AdResponse createAd(AdCreateRequest request) {
        User currentUser = userService.getCurrentUserEntity();
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("категория не найдена"));

        // новое объявление создаётся как DRAFT, затем отправляется на модерацию
        Ad ad = Ad.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .region(request.getRegion())
                .category(category)
                .user(currentUser)
                .status(Ad.Status.DRAFT)
                .build();

        ad = adRepository.save(ad);
        log.info("создано объявление: id={}, userId={}", ad.getId(), currentUser.getId());

        // авто-submit на модерацию
        transitionService.submitForModeration(ad.getId());

        return mapAdToResponse(ad);
    }

    @Transactional
    public AdResponse updateAd(Long id, AdUpdateRequest request) {
        Ad ad = adRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("объявление не найдено"));

        Long currentUserId = userService.getCurrentUserId();
        if (!ad.getUser().getId().equals(currentUserId)) {
            throw new IllegalArgumentException("нет прав на редактирование");
        }

        if (request.getTitle() != null) ad.setTitle(request.getTitle());
        if (request.getDescription() != null) ad.setDescription(request.getDescription());
        if (request.getPrice() != null) ad.setPrice(request.getPrice());
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("категория не найдена"));
            ad.setCategory(category);
        }

        ad = adRepository.save(ad);

        // любое редактирование PUBLISHED/REJECTED → повторная модерация
        if (ad.getStatus() == Ad.Status.PUBLISHED || ad.getStatus() == Ad.Status.REJECTED) {
            transitionService.resubmitAfterEdit(ad.getId());
        }

        log.info("обновлено объявление: id={}", ad.getId());
        return mapAdToResponse(ad);
    }

    @Transactional
    public void deleteAd(Long id) {
        transitionService.removeAd(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdResponse> getMyAds(int page, int size) {
        Long currentUserId = userService.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Ad> adPage = adRepository.findByOwner(currentUserId, pageable);
        return toPageResponse(adPage);
    }

    // кабинет владельца с lifecycle-данными
    @Transactional(readOnly = true)
    public PageResponse<MyAdResponse> getMyAdsTyped(int page, int size, String statusFilter) {
        Long currentUserId = userService.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Ad> adPage;
        if (statusFilter != null && !statusFilter.isBlank()) {
            Ad.Status status = Ad.Status.valueOf(statusFilter.toUpperCase());
            adPage = adRepository.findByUserIdAndStatus(currentUserId, status, pageable);
        } else {
            adPage = adRepository.findByOwner(currentUserId, pageable);
        }

        List<MyAdResponse> content = adPage.getContent().stream()
                .map(this::mapToMyAdResponse)
                .collect(Collectors.toList());

        return PageResponse.<MyAdResponse>builder()
                .content(content)
                .page(adPage.getNumber())
                .size(adPage.getSize())
                .totalElements(adPage.getTotalElements())
                .totalPages(adPage.getTotalPages())
                .last(adPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<AdResponse> searchAds(AdSearchRequest request) {
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Specification<Ad> spec = buildSearchSpec(request);
        Page<Ad> adPage = adRepository.findAll(spec, pageable);
        return toPageResponse(adPage);
    }

    /** Все объявления (включая удалённые) — для админ-панели */
    @Transactional(readOnly = true)
    public PageResponse<AdResponse> getAllAdsForAdmin(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Ad> adPage = adRepository.findAll(pageable);
        return toPageResponse(adPage);
    }

    public long countAll() {
        return adRepository.count();
    }

    private Specification<Ad> buildSearchSpec(AdSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // публичный поиск — только PUBLISHED
            predicates.add(cb.equal(root.get("status"), Ad.Status.PUBLISHED));

            // текстовый поиск
            if (request.getQuery() != null && !request.getQuery().isBlank()) {
                String pattern = "%" + request.getQuery().toLowerCase() + "%";
                if (Boolean.TRUE.equals(request.getTitleOnly())) {
                    predicates.add(cb.like(cb.lower(root.get("title")), pattern));
                } else {
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("title")), pattern),
                            cb.like(cb.lower(root.get("description")), pattern)
                    ));
                }
            }

            // фильтр по категории
            if (request.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), request.getCategoryId()));
            }

            // фильтр по цене
            if (request.getPriceFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), request.getPriceFrom()));
            }
            if (request.getPriceTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), request.getPriceTo()));
            }

            // фильтр по региону
            if (request.getRegion() != null && !request.getRegion().isBlank()) {
                predicates.add(cb.equal(root.get("region"), request.getRegion()));
            }

            // фильтр по пользователю
            if (request.getUserId() != null) {
                predicates.add(cb.equal(root.get("user").get("id"), request.getUserId()));
            }

            // только с фото
            if (Boolean.TRUE.equals(request.getWithPhoto())) {
                predicates.add(cb.isNotEmpty(root.get("images")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public AdResponse mapAdToResponse(Ad ad) {
        List<AdResponse.ImageResponse> images = null;
        if (ad.getImages() != null) {
            images = ad.getImages().stream()
                    .map(img -> AdResponse.ImageResponse.builder()
                            .id(img.getId())
                            .url("/images/" + img.getId())
                            .sortOrder(img.getSortOrder())
                            .build())
                    .collect(Collectors.toList());
        }

        // url первого изображения для превью
        String imageUrl = (images != null && !images.isEmpty()) ? images.get(0).getUrl() : null;

        return AdResponse.builder()
                .id(ad.getId())
                .title(ad.getTitle())
                .description(ad.getDescription())
                .price(ad.getPrice())
                .categoryId(ad.getCategory().getId())
                .categoryName(ad.getCategory().getName())
                .userId(ad.getUser().getId())
                .userName(ad.getUser().getName())
                .region(ad.getRegion())
                .status(ad.getStatus().name())
                .imageUrl(imageUrl)
                .images(images)
                .commentCount(ad.getComments() != null ? ad.getComments().size() : 0)
                .viewCount(ad.getViewCount() != null ? ad.getViewCount() : 0L)
                .createdAt(ad.getCreatedAt())
                .updatedAt(ad.getUpdatedAt())
                .build();
    }

    private MyAdResponse mapToMyAdResponse(Ad ad) {
        List<AdResponse.ImageResponse> images = mapImages(ad);
        String imageUrl = (images != null && !images.isEmpty()) ? images.get(0).getUrl() : null;

        return MyAdResponse.builder()
                .id(ad.getId())
                .title(ad.getTitle())
                .description(ad.getDescription())
                .price(ad.getPrice())
                .categoryId(ad.getCategory() != null ? ad.getCategory().getId() : null)
                .categoryName(ad.getCategory() != null ? ad.getCategory().getName() : null)
                .region(ad.getRegion())
                .status(ad.getStatus().name())
                .saleType(ad.getSaleType() != null ? ad.getSaleType().name() : null)
                .rejectionReason(ad.getRejectionReason())
                .imageUrl(imageUrl)
                .images(images)
                .viewCount(ad.getViewCount() != null ? ad.getViewCount() : 0L)
                .submittedAt(ad.getSubmittedAt())
                .publishedAt(ad.getPublishedAt())
                .createdAt(ad.getCreatedAt())
                .updatedAt(ad.getUpdatedAt())
                .build();
    }

    private List<AdResponse.ImageResponse> mapImages(Ad ad) {
        if (ad.getImages() == null) return null;
        return ad.getImages().stream()
                .map(img -> AdResponse.ImageResponse.builder()
                        .id(img.getId())
                        .url("/images/" + img.getId())
                        .sortOrder(img.getSortOrder())
                        .build())
                .collect(Collectors.toList());
    }

    private PageResponse<AdResponse> toPageResponse(Page<Ad> adPage) {
        List<AdResponse> content = adPage.getContent().stream()
                .map(this::mapAdToResponse)
                .collect(Collectors.toList());

        return PageResponse.<AdResponse>builder()
                .content(content)
                .page(adPage.getNumber())
                .size(adPage.getSize())
                .totalElements(adPage.getTotalElements())
                .totalPages(adPage.getTotalPages())
                .last(adPage.isLast())
                .build();
    }
}
