package com.solarl.nado.service;

import com.solarl.nado.dto.request.CommentCreateRequest;
import com.solarl.nado.dto.response.CommentResponse;
import com.solarl.nado.dto.response.PageResponse;
import com.solarl.nado.entity.Ad;
import com.solarl.nado.entity.Comment;
import com.solarl.nado.entity.User;
import com.solarl.nado.exception.ResourceNotFoundException;
import com.solarl.nado.repository.AdRepository;
import com.solarl.nado.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final AdRepository adRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getCommentsByAdId(Long adId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> commentPage = commentRepository.findByAdIdOrderByCreatedAtDesc(adId, pageable);

        List<CommentResponse> content = commentPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<CommentResponse>builder()
                .content(content)
                .page(commentPage.getNumber())
                .size(commentPage.getSize())
                .totalElements(commentPage.getTotalElements())
                .totalPages(commentPage.getTotalPages())
                .last(commentPage.isLast())
                .build();
    }

    @Transactional
    public CommentResponse createComment(Long adId, CommentCreateRequest request) {
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> new ResourceNotFoundException("Объявление не найдено"));

        User currentUser = userService.getCurrentUserEntity();

        Comment comment = Comment.builder()
                .content(request.getContent())
                .ad(ad)
                .user(currentUser)
                .build();

        comment = commentRepository.save(comment);
        log.info("Создан комментарий: id={}, adId={}, userId={}", comment.getId(), adId, currentUser.getId());
        return mapToResponse(comment);
    }

    private CommentResponse mapToResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .userId(comment.getUser().getId())
                .userName(comment.getUser().getName())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
