package com.solarl.nado.service;

import com.solarl.nado.dto.request.CommentCreateRequest;
import com.solarl.nado.dto.response.CommentResponse;
import com.solarl.nado.dto.response.PageResponse;
import com.solarl.nado.entity.Ad;
import com.solarl.nado.entity.Category;
import com.solarl.nado.entity.Comment;
import com.solarl.nado.entity.User;
import com.solarl.nado.exception.ResourceNotFoundException;
import com.solarl.nado.repository.AdRepository;
import com.solarl.nado.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private AdRepository adRepository;
    @Mock
    private UserService userService;

    @InjectMocks
    private CommentService commentService;

    private User testUser;
    private Ad testAd;
    private Comment testComment;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L).name("Иван").email("ivan@test.com")
                .role(User.Role.USER).active(true).build();

        Category category = Category.builder()
                .id(1L).name("Транспорт").children(new ArrayList<>()).build();

        testAd = Ad.builder()
                .id(1L).title("Тест").description("Тест")
                .category(category).user(testUser).status(Ad.Status.PUBLISHED)
                .images(new ArrayList<>()).comments(new ArrayList<>())
                .build();

        testComment = Comment.builder()
                .id(1L).content("Отличное объявление!")
                .ad(testAd).user(testUser)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Создание комментария: успешно")
    void createComment_success() {
        // подготовка
        CommentCreateRequest request = new CommentCreateRequest();
        request.setContent("Отличное объявление!");

        when(adRepository.findById(1L)).thenReturn(Optional.of(testAd));
        when(userService.getCurrentUserEntity()).thenReturn(testUser);
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(1L);
            c.setCreatedAt(LocalDateTime.now());
            return c;
        });

        // выполнение
        CommentResponse response = commentService.createComment(1L, request);

        // проверка
        assertNotNull(response);
        assertEquals("Отличное объявление!", response.getContent());
        assertEquals("Иван", response.getUserName());
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("Создание комментария: объявление не найдено — ошибка")
    void createComment_adNotFound_throwsException() {
        // подготовка
        CommentCreateRequest request = new CommentCreateRequest();
        request.setContent("Тест");

        when(adRepository.findById(999L)).thenReturn(Optional.empty());

        // выполнение и проверка
        assertThrows(ResourceNotFoundException.class,
                () -> commentService.createComment(999L, request));
    }

    @Test
    @DisplayName("Получение комментариев: пагинация")
    void getCommentsByAdId_returnsPaginatedResult() {
        // подготовка
        Page<Comment> page = new PageImpl<>(List.of(testComment));
        when(commentRepository.findByAdIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(page);

        // выполнение
        PageResponse<CommentResponse> response = commentService.getCommentsByAdId(1L, 0, 20);

        // проверка
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("Отличное объявление!", response.getContent().get(0).getContent());
    }
}
