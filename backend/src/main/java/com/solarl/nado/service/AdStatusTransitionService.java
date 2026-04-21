package com.solarl.nado.service;

import com.solarl.nado.entity.Ad;
import com.solarl.nado.entity.User;
import com.solarl.nado.exception.ResourceNotFoundException;
import com.solarl.nado.repository.AdRepository;
import com.solarl.nado.security.AuthFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * единственный центр правил перехода статуса объявления.
 * ни AdService, ни ModerationService, ни контроллеры не меняют Ad.status напрямую —
 * только через этот сервис.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdStatusTransitionService {

    private final AdRepository adRepository;
    private final AuthFacade authFacade;
    private final WantedMatchingService wantedMatchingService;
    private final NotificationService notificationService;

    // допустимые переходы: from → set(to)
    private static final Map<Ad.Status, Set<Ad.Status>> TRANSITIONS = Map.of(
        Ad.Status.DRAFT,                Set.of(Ad.Status.PENDING_MODERATION, Ad.Status.REMOVED),
        Ad.Status.PENDING_MODERATION,   Set.of(Ad.Status.PUBLISHED, Ad.Status.REJECTED, Ad.Status.BLOCKED, Ad.Status.REMOVED),
        Ad.Status.PUBLISHED,            Set.of(Ad.Status.PENDING_MODERATION, Ad.Status.SOLD, Ad.Status.ARCHIVED, Ad.Status.BLOCKED, Ad.Status.REMOVED),
        Ad.Status.REJECTED,             Set.of(Ad.Status.PENDING_MODERATION, Ad.Status.REMOVED),
        Ad.Status.SOLD,                 Set.of(Ad.Status.ARCHIVED, Ad.Status.REMOVED),
        Ad.Status.ARCHIVED,             Set.of(Ad.Status.REMOVED),
        Ad.Status.BLOCKED,              Set.of(Ad.Status.PUBLISHED, Ad.Status.REMOVED),
        Ad.Status.REMOVED,              Set.of()
    );

    private static final Set<Ad.Status> OWNER_TRANSITIONS = Set.of(
        Ad.Status.PENDING_MODERATION, Ad.Status.SOLD, Ad.Status.ARCHIVED, Ad.Status.REMOVED
    );

    private static final Set<Ad.Status> MODERATOR_TRANSITIONS = Set.of(
        Ad.Status.PUBLISHED, Ad.Status.REJECTED, Ad.Status.BLOCKED, Ad.Status.REMOVED
    );



    @Transactional
    public Ad submitForModeration(Long adId) {
        Ad ad = findAd(adId);
        User owner = authFacade.getCurrentNadoUser();
        checkOwner(ad, owner);

        return transition(ad, Ad.Status.PENDING_MODERATION, owner, null);
    }

    @Transactional
    public Ad markSold(Long adId) {
        Ad ad = findAd(adId);
        User owner = authFacade.getCurrentNadoUser();
        checkOwner(ad, owner);

        return transition(ad, Ad.Status.SOLD, owner, null);
    }

    @Transactional
    public Ad archiveAd(Long adId) {
        Ad ad = findAd(adId);
        User owner = authFacade.getCurrentNadoUser();
        checkOwner(ad, owner);

        return transition(ad, Ad.Status.ARCHIVED, owner, null);
    }

    // soft-delete: сохраняется в history для аудита
    @Transactional
    public Ad removeAd(Long adId) {
        Ad ad = findAd(adId);
        User actor = authFacade.getCurrentNadoUser();

        boolean isOwner = ad.getUser().getId().equals(actor.getId());
        boolean isMod = actor.getRole() == User.Role.MODERATOR || actor.getRole() == User.Role.ADMIN;

        if (!isOwner && !isMod) {
            throw new IllegalArgumentException("нет прав на удаление");
        }

        return transition(ad, Ad.Status.REMOVED, actor, null);
    }

    // после правки опубликованного — повторная модерация
    @Transactional
    public Ad resubmitAfterEdit(Long adId) {
        Ad ad = findAd(adId);
        User owner = authFacade.getCurrentNadoUser();
        checkOwner(ad, owner);

        if (ad.getStatus() != Ad.Status.PUBLISHED && ad.getStatus() != Ad.Status.REJECTED) {
            throw new IllegalStateException("редактирование с повторной отправкой возможно только для PUBLISHED или REJECTED");
        }

        return transition(ad, Ad.Status.PENDING_MODERATION, owner, null);
    }



    @Transactional
    public Ad approve(Long adId) {
        Ad ad = findAd(adId);
        User moderator = requireModerator();

        Ad result = transition(ad, Ad.Status.PUBLISHED, moderator, null);
        result.setPublishedAt(LocalDateTime.now());
        return adRepository.save(result);
    }

    @Transactional
    public Ad reject(Long adId, String reason) {
        Ad ad = findAd(adId);
        User moderator = requireModerator();

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("причина отклонения обязательна");
        }

        Ad result = transition(ad, Ad.Status.REJECTED, moderator, reason);
        return result;
    }

    @Transactional
    public Ad blockAd(Long adId, String reason) {
        Ad ad = findAd(adId);
        User moderator = requireModerator();

        Ad result = transition(ad, Ad.Status.BLOCKED, moderator, reason);
        return result;
    }



    private Ad transition(Ad ad, Ad.Status to, User actor, String reason) {
        Ad.Status from = ad.getStatus();

        Set<Ad.Status> allowed = TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new IllegalStateException(
                    String.format("переход %s → %s недопустим", from, to));
        }

        ad.setStatus(to);
        ad.setModeratedAt(LocalDateTime.now());

        if (to == Ad.Status.PENDING_MODERATION) {
            ad.setSubmittedAt(LocalDateTime.now());
            ad.setRejectionReason(null);
        }

        if (to == Ad.Status.REJECTED) {
            ad.setRejectionReason(reason);
            ad.setModeratedBy(actor);
        }

        if (to == Ad.Status.PUBLISHED) {
            ad.setModeratedBy(actor);
            ad.setRejectionReason(null);
        }

        if (to == Ad.Status.BLOCKED) {
            ad.setRejectionReason(reason);
            ad.setModeratedBy(actor);
        }

        ad = adRepository.save(ad);

        log.info("AUDIT: ad id={} {} → {}, actor={} ({})",
                ad.getId(), from, to, actor.getId(), actor.getRole());

        // публикация: запуск wanted matching + уведомление владельцу
        if (to == Ad.Status.PUBLISHED) {
            wantedMatchingService.matchPublishedAd(ad);
            notificationService.notifyAdApproved(ad.getUser(), ad.getId(), ad.getTitle());
        }

        if (to == Ad.Status.REJECTED) {
            notificationService.notifyAdRejected(ad.getUser(), ad.getId(), ad.getTitle(), reason);
        }

        if (to == Ad.Status.BLOCKED) {
            notificationService.notifyAdBlocked(ad.getUser(), ad.getId(), ad.getTitle(), reason);
        }

        return ad;
    }



    private Ad findAd(Long adId) {
        return adRepository.findById(adId)
                .orElseThrow(() -> new ResourceNotFoundException("объявление не найдено"));
    }

    private void checkOwner(Ad ad, User actor) {
        if (!ad.getUser().getId().equals(actor.getId())) {
            throw new IllegalArgumentException("действие доступно только владельцу объявления");
        }
    }

    private User requireModerator() {
        User user = authFacade.getCurrentNadoUser();
        if (user.getRole() != User.Role.MODERATOR && user.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("действие доступно только модератору");
        }
        return user;
    }
}
