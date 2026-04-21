package com.solarl.nado.service;

import com.solarl.nado.config.StorageProperties;
import com.solarl.nado.dto.response.UserPrivateResponse;
import com.solarl.nado.dto.response.UserPublicResponse;
import com.solarl.nado.entity.User;
import com.solarl.nado.exception.ResourceNotFoundException;
import com.solarl.nado.repository.AdRepository;
import com.solarl.nado.repository.RatingRepository;
import com.solarl.nado.repository.UserRepository;
import com.solarl.nado.security.AuthFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AdRepository adRepository;
    @Mock
    private RatingRepository ratingRepository;
    @Mock
    private StorageProperties storageProperties;
    @Mock
    private FileValidationService fileValidationService;
    @Mock
    private AuthFacade authFacade;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L).name("Иван").email("ivan@test.com").phone("+79001234567")
                .role(User.Role.USER).active(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Получение текущего пользователя: возвращает приватный профиль с email")
    void getCurrentUser_returnsPrivateResponse() {
        when(authFacade.getCurrentUserId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserPrivateResponse response = userService.getCurrentUser();

        assertNotNull(response);
        assertEquals("Иван", response.getName());
        assertEquals("ivan@test.com", response.getEmail());
        assertEquals("+79001234567", response.getPhone());
    }

    @Test
    @DisplayName("Получение пользователя по ID: возвращает публичный профиль БЕЗ email/phone")
    void getUserById_returnsPublicResponse_withoutEmailPhone() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserPublicResponse response = userService.getUserById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Иван", response.getName());
    }

    @Test
    @DisplayName("Получение пользователя по ID: не найден — ошибка")
    void getUserById_notFound_throwsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserById(999L));
    }

    @Test
    @DisplayName("getCurrentUserId делегирует в AuthFacade")
    void getCurrentUserId_delegatesToAuthFacade() {
        when(authFacade.getCurrentUserId()).thenReturn(42L);

        Long userId = userService.getCurrentUserId();

        assertEquals(42L, userId);
        verify(authFacade).getCurrentUserId();
    }

    @Test
    @DisplayName("Маскировка телефона работает корректно")
    void revealPhone_masksCorrectly() {
        var response = userService.revealPhone("+79001234567");

        assertTrue(response.isRevealed());
        assertEquals("+79001234567", response.getPhone());
        assertNotNull(response.getMasked());
        assertTrue(response.getMasked().contains("***"));
    }

    @Test
    @DisplayName("Раскрытие пустого телефона: revealed=false")
    void revealPhone_null_returnsNotRevealed() {
        var response = userService.revealPhone(null);

        assertFalse(response.isRevealed());
        assertNull(response.getPhone());
    }
}
