package com.solarl.nado.controller;

import com.solarl.nado.dto.request.CommentCreateRequest;
import com.solarl.nado.dto.response.CommentResponse;
import com.solarl.nado.dto.response.PageResponse;
import com.solarl.nado.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/ads/{adId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * Все пользователи — просмотр комментариев к объявлению
     */
    @GetMapping
    public ResponseEntity<PageResponse<CommentResponse>> getComments(
            @PathVariable Long adId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(commentService.getCommentsByAdId(adId, page, size));
    }

    /**
     * Авторизованный — оставить комментарий
     */
    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long adId,
            @Valid @RequestBody CommentCreateRequest request) {
        CommentResponse response = commentService.createComment(adId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
