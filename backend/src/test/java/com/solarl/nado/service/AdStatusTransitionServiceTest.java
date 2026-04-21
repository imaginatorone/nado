package com.solarl.nado.service;

import com.solarl.nado.entity.Ad;
import com.solarl.nado.entity.User;
import com.solarl.nado.repository.AdRepository;
import com.solarl.nado.security.AuthFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdStatusTransitionServiceTest {

    @Mock
    private AdRepository adRepository;
    @Mock
    private AuthFacade authFacade;
    @Mock
    private WantedMatchingService wantedMatchingService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AdStatusTransitionService transitionService;

    private User owner;
    private User moderator;
    private User stranger;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).name("Иван").email("ivan@test.com")
                .role(User.Role.USER).build();
        moderator = User.builder().id(2L).name("Модератор").email("mod@test.com")
                .role(User.Role.MODERATOR).build();
        stranger = User.builder().id(3L).name("Чужой").email("other@test.com")
                .role(User.Role.USER).build();
    }

    private Ad createAd(Ad.Status status) {
        return Ad.builder().id(10L).title("Тест").description("Тест")
                .user(owner).status(status).build();
    }

    @Test
    @DisplayName("DRAFT → PENDING_MODERATION: owner отправляет на модерацию")
    void submitForModeration_fromDraft() {
        Ad ad = createAd(Ad.Status.DRAFT);
        when(adRepository.findById(10L)).thenReturn(Optional.of(ad));
        when(authFacade.getCurrentNadoUser()).thenReturn(owner);
        when(adRepository.save(any(Ad.class))).thenAnswer(inv -> inv.getArgument(0));

        Ad result = transitionService.submitForModeration(10L);

        assertEquals(Ad.Status.PENDING_MODERATION, result.getStatus());
        assertNotNull(result.getSubmittedAt());
    }

    @Test
    @DisplayName("REJECTED → PENDING_MODERATION: owner перeотправляет после правки")
    void resubmitAfterEdit_fromRejected() {
        Ad ad = createAd(Ad.Status.REJECTED);
        ad.setRejectionReason("плохие фото");
        when(adRepository.findById(10L)).thenReturn(Optional.of(ad));
        when(authFacade.getCurrentNadoUser()).thenReturn(owner);
        when(adRepository.save(any(Ad.class))).thenAnswer(inv -> inv.getArgument(0));

        Ad result = transitionService.resubmitAfterEdit(10L);

        assertEquals(Ad.Status.PENDING_MODERATION, result.getStatus());
        assertNull(result.getRejectionReason());
    }

    @Test
    @DisplayName("PENDING_MODERATION → PUBLISHED: модератор одобряет")
    void approve_byModerator() {
        Ad ad = createAd(Ad.Status.PENDING_MODERATION);
        when(adRepository.findById(10L)).thenReturn(Optional.of(ad));
        when(authFacade.getCurrentNadoUser()).thenReturn(moderator);
        when(adRepository.save(any(Ad.class))).thenAnswer(inv -> inv.getArgument(0));

        Ad result = transitionService.approve(10L);

        assertEquals(Ad.Status.PUBLISHED, result.getStatus());
        assertNotNull(result.getPublishedAt());
        assertEquals(moderator, result.getModeratedBy());
    }

    @Test
    @DisplayName("PENDING_MODERATION → REJECTED: модератор отклоняет с причиной")
    void reject_byModerator() {
        Ad ad = createAd(Ad.Status.PENDING_MODERATION);
        when(adRepository.findById(10L)).thenReturn(Optional.of(ad));
        when(authFacade.getCurrentNadoUser()).thenReturn(moderator);
        when(adRepository.save(any(Ad.class))).thenAnswer(inv -> inv.getArgument(0));

        Ad result = transitionService.reject(10L, "нарушение правил");

        assertEquals(Ad.Status.REJECTED, result.getStatus());
        assertEquals("нарушение правил", result.getRejectionReason());
    }

    @Test
    @DisplayName("PUBLISHED → PENDING_MODERATION: правка → повторная модерация")
    void resubmitAfterEdit_fromPublished() {
        Ad ad = createAd(Ad.Status.PUBLISHED);
        when(adRepository.findById(10L)).thenReturn(Optional.of(ad));
        when(authFacade.getCurrentNadoUser()).thenReturn(owner);
        when(adRepository.save(any(Ad.class))).thenAnswer(inv -> inv.getArgument(0));

        Ad result = transitionService.resubmitAfterEdit(10L);

        assertEquals(Ad.Status.PENDING_MODERATION, result.getStatus());
    }

    @Test
    @DisplayName("Недопустимый переход: DRAFT → PUBLISHED — ошибка")
    void invalidTransition_draftToPublished() {
        Ad ad = createAd(Ad.Status.DRAFT);
        when(adRepository.findById(10L)).thenReturn(Optional.of(ad));
        when(authFacade.getCurrentNadoUser()).thenReturn(moderator);

        assertThrows(IllegalStateException.class,
                () -> transitionService.approve(10L));
    }

    @Test
    @DisplayName("Чужой пользователь не может отправить на модерацию")
    void submitForModeration_byStranger_throws() {
        Ad ad = createAd(Ad.Status.DRAFT);
        when(adRepository.findById(10L)).thenReturn(Optional.of(ad));
        when(authFacade.getCurrentNadoUser()).thenReturn(stranger);

        assertThrows(IllegalArgumentException.class,
                () -> transitionService.submitForModeration(10L));
    }

    @Test
    @DisplayName("reject без причины — ошибка")
    void reject_withoutReason_throws() {
        Ad ad = createAd(Ad.Status.PENDING_MODERATION);
        when(adRepository.findById(10L)).thenReturn(Optional.of(ad));
        when(authFacade.getCurrentNadoUser()).thenReturn(moderator);

        assertThrows(IllegalArgumentException.class,
                () -> transitionService.reject(10L, ""));
    }

    @Test
    @DisplayName("USER не может одобрить объявление")
    void approve_byRegularUser_throws() {
        Ad ad = createAd(Ad.Status.PENDING_MODERATION);
        when(adRepository.findById(10L)).thenReturn(Optional.of(ad));
        when(authFacade.getCurrentNadoUser()).thenReturn(owner);

        assertThrows(IllegalArgumentException.class,
                () -> transitionService.approve(10L));
    }

    @Test
    @DisplayName("REMOVED — терминальный статус, переходов нет")
    void removeAd_isTerminal() {
        Ad ad = createAd(Ad.Status.PUBLISHED);
        when(adRepository.findById(10L)).thenReturn(Optional.of(ad));
        when(authFacade.getCurrentNadoUser()).thenReturn(owner);
        when(adRepository.save(any(Ad.class))).thenAnswer(inv -> inv.getArgument(0));

        Ad removed = transitionService.removeAd(10L);
        assertEquals(Ad.Status.REMOVED, removed.getStatus());
    }
}
