package com.solarl.nado.service;

import com.solarl.nado.config.StorageProperties;
import com.solarl.nado.dto.response.ChatMessageResponse;
import com.solarl.nado.dto.response.ChatRoomResponse;
import com.solarl.nado.entity.*;
import com.solarl.nado.exception.ResourceNotFoundException;
import com.solarl.nado.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AdRepository adRepository;
    private final UserRepository userRepository;
    private final StorageProperties storageProperties;
    private final FileValidationService fileValidationService;

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getUserChats(Long userId) {
        List<ChatRoom> rooms = chatRoomRepository.findAllByUserId(userId);
        return rooms.stream().map(room -> {
            User otherUser = room.getBuyer().getId().equals(userId) ? room.getSeller() : room.getBuyer();
            List<ChatMessage> messages = room.getMessages();
            ChatMessage lastMsg = messages.isEmpty() ? null : messages.get(messages.size() - 1);
            long unread = chatMessageRepository.countUnreadInRoom(room.getId(), userId);

            String adImageUrl = null;
            Ad ad = room.getAd();
            if (ad.getImages() != null && !ad.getImages().isEmpty()) {
                adImageUrl = "/images/" + ad.getImages().get(0).getId();
            }

            return ChatRoomResponse.builder()
                    .id(room.getId())
                    .adId(ad.getId())
                    .adTitle(ad.getTitle())
                    .adImageUrl(adImageUrl)
                    .otherUserId(otherUser.getId())
                    .otherUserName(otherUser.getName())
                    .lastMessage(lastMsg != null ? (lastMsg.getContent() != null ? lastMsg.getContent() : "📎 Вложение") : null)
                    .lastMessageAt(lastMsg != null ? lastMsg.getCreatedAt() : room.getCreatedAt())
                    .unreadCount(unread)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public ChatRoomResponse getOrCreateRoom(Long adId, Long buyerId) {
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> new ResourceNotFoundException("Объявление не найдено"));

        if (ad.getUser().getId().equals(buyerId)) {
            throw new IllegalArgumentException("Нельзя написать себе");
        }

        ChatRoom room = chatRoomRepository.findByAdIdAndBuyerId(adId, buyerId)
                .orElseGet(() -> {
                    ChatRoom newRoom = ChatRoom.builder()
                            .ad(ad)
                            .buyer(userRepository.findById(buyerId).orElseThrow())
                            .seller(ad.getUser())
                            .build();
                    return chatRoomRepository.save(newRoom);
                });

        User otherUser = room.getBuyer().getId().equals(buyerId) ? room.getSeller() : room.getBuyer();

        return ChatRoomResponse.builder()
                .id(room.getId())
                .adId(ad.getId())
                .adTitle(ad.getTitle())
                .otherUserId(otherUser.getId())
                .otherUserName(otherUser.getName())
                .unreadCount(0)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getRoomMessages(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Чат не найден"));

        if (!room.getBuyer().getId().equals(userId) && !room.getSeller().getId().equals(userId)) {
            throw new AccessDeniedException("Нет доступа к чату");
        }

        List<ChatMessage> messages = chatMessageRepository.findByRoomIdOrderByCreatedAtAsc(roomId);
        return messages.stream().map(msg -> ChatMessageResponse.builder()
                .id(msg.getId())
                .senderId(msg.getSender().getId())
                .senderName(msg.getSender().getName())
                .content(msg.getContent())
                // исправлен URL: /chats/files/ — соответствует ChatController
                .attachmentUrl(msg.getAttachmentPath() != null ? "/chats/files/" + msg.getId() : null)
                .attachmentType(msg.getAttachmentType())
                .read(msg.isRead())
                .mine(msg.getSender().getId().equals(userId))
                .createdAt(msg.getCreatedAt())
                .build()
        ).collect(Collectors.toList());
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long roomId, Long senderId, String content, MultipartFile file) {
        // валидация: требуется или текст, или вложение
        boolean hasContent = content != null && !content.isBlank();
        boolean hasFile = file != null && !file.isEmpty();
        if (!hasContent && !hasFile) {
            throw new IllegalArgumentException("сообщение должно содержать текст или вложение");
        }

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Чат не найден"));

        if (!room.getBuyer().getId().equals(senderId) && !room.getSeller().getId().equals(senderId)) {
            throw new AccessDeniedException("Нет доступа к чату");
        }

        User sender = userRepository.findById(senderId).orElseThrow();

        ChatMessage msg = ChatMessage.builder()
                .room(room)
                .sender(sender)
                .content(content)
                .read(false)
                .build();

        if (file != null && !file.isEmpty()) {
            fileValidationService.validateFile(file);
            try {
                String ext = fileValidationService.getExtension(file.getOriginalFilename());
                String filename = UUID.randomUUID() + ext;
                Path chatDir = Paths.get(storageProperties.getDir(), "chat");
                Files.createDirectories(chatDir);
                Path filePath = chatDir.resolve(filename);
                file.transferTo(filePath.toFile());
                msg.setAttachmentPath(filePath.toString());
                // MIME определяется по содержимому, а не из запроса
                msg.setAttachmentType(fileValidationService.detectMimeType(filePath));
            } catch (IOException e) {
                throw new RuntimeException("Ошибка загрузки файла", e);
            }
        }

        msg = chatMessageRepository.save(msg);

        return ChatMessageResponse.builder()
                .id(msg.getId())
                .senderId(senderId)
                .senderName(sender.getName())
                .content(msg.getContent())
                .attachmentUrl(msg.getAttachmentPath() != null ? "/chats/files/" + msg.getId() : null)
                .attachmentType(msg.getAttachmentType())
                .read(false)
                .mine(true)
                .createdAt(msg.getCreatedAt())
                .build();
    }

    @Transactional
    public void markAsRead(Long roomId, Long userId) {
        chatMessageRepository.markAsRead(roomId, userId);
    }

    @Transactional(readOnly = true)
    public long getTotalUnread(Long userId) {
        return chatMessageRepository.countTotalUnread(userId);
    }

    /**
     * Object-level authorization для доступа к вложению чата.
     * Проверяет: сообщение существует, имеет вложение, пользователь - участник комнаты.
     */
    @Transactional(readOnly = true)
    public ChatMessage getAuthorizedAttachment(Long messageId, Long userId) {
        ChatMessage msg = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Сообщение не найдено"));

        if (msg.getAttachmentPath() == null) {
            throw new ResourceNotFoundException("Вложение не найдено");
        }

        ChatRoom room = msg.getRoom();
        if (!room.getBuyer().getId().equals(userId) && !room.getSeller().getId().equals(userId)) {
            throw new AccessDeniedException("Нет доступа к файлу");
        }

        return msg;
    }

    // проверка участия в комнате для websocket subscribe auth
    @Transactional(readOnly = true)
    public boolean isRoomMember(Long roomId, Long userId) {
        return chatRoomRepository.findById(roomId)
                .map(room -> room.getBuyer().getId().equals(userId)
                        || room.getSeller().getId().equals(userId))
                .orElse(false);
    }

    // получатель сообщения (не sender) для ws broadcast
    @Transactional(readOnly = true)
    public Long getRecipientId(Long roomId, Long senderId) {
        ChatRoom room = chatRoomRepository.findById(roomId).orElse(null);
        if (room == null) return null;
        return room.getBuyer().getId().equals(senderId)
                ? room.getSeller().getId()
                : room.getBuyer().getId();
    }
}
