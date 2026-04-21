package com.solarl.nado.controller;

import com.solarl.nado.dto.response.ChatMessageResponse;
import com.solarl.nado.dto.response.ChatRoomResponse;
import com.solarl.nado.dto.request.StartChatRequest;
import com.solarl.nado.entity.ChatMessage;
import com.solarl.nado.service.ChatService;
import com.solarl.nado.service.FileValidationService;
import com.solarl.nado.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;
    private final FileValidationService fileValidationService;

    @GetMapping
    public ResponseEntity<List<ChatRoomResponse>> getMyChats() {
        return ResponseEntity.ok(chatService.getUserChats(userService.getCurrentUserId()));
    }

    @PostMapping("/start")
    public ResponseEntity<ChatRoomResponse> startChat(@Valid @RequestBody StartChatRequest request) {
        return ResponseEntity.ok(chatService.getOrCreateRoom(request.getAdId(), userService.getCurrentUserId()));
    }

    @GetMapping("/{roomId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(@PathVariable Long roomId) {
        return ResponseEntity.ok(chatService.getRoomMessages(roomId, userService.getCurrentUserId()));
    }

    @PostMapping("/{roomId}/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable Long roomId,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) MultipartFile file) {
        return ResponseEntity.ok(chatService.sendMessage(roomId, userService.getCurrentUserId(), content, file));
    }

    @PutMapping("/{roomId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long roomId) {
        chatService.markAsRead(roomId, userService.getCurrentUserId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        return ResponseEntity.ok(Map.of("count", chatService.getTotalUnread(userService.getCurrentUserId())));
    }

    /**
     * object-level auth: доступ к файлу только участникам комнаты.
     * inline только для изображений — остальное как attachment.
     */
    @GetMapping("/files/{messageId}")
    public ResponseEntity<Resource> getChatFile(@PathVariable Long messageId) {
        Long userId = userService.getCurrentUserId();
        ChatMessage msg = chatService.getAuthorizedAttachment(messageId, userId);

        Path filePath = Paths.get(msg.getAttachmentPath());
        Resource resource = new FileSystemResource(filePath);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String mimeType = fileValidationService.detectMimeType(filePath);
        String disposition = fileValidationService.safeContentDisposition(
                mimeType, filePath.getFileName().toString());

        log.info("AUDIT: user id={} скачал вложение messageId={}", userId, messageId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=86400")
                .body(resource);
    }
}
