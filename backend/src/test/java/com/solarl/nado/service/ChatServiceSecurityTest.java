package com.solarl.nado.service;

import com.solarl.nado.config.StorageProperties;
import com.solarl.nado.entity.*;
import com.solarl.nado.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * security proof для чата: object-level authorization.
 * доказывает что outsider не может отправить / получить / подписаться.
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceSecurityTest {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private AdRepository adRepository;
    @Mock private UserRepository userRepository;
    @Mock private StorageProperties storageProperties;
    @Mock private FileValidationService fileValidationService;

    @InjectMocks private ChatService chatService;

    private User buyer;
    private User seller;
    private User outsider;
    private ChatRoom room;

    @BeforeEach
    void setUp() {
        buyer = User.builder().id(1L).name("Покупатель").build();
        seller = User.builder().id(2L).name("Продавец").build();
        outsider = User.builder().id(99L).name("Злоумышленник").build();

        Ad ad = Ad.builder().id(10L).title("Тест").build();

        room = ChatRoom.builder()
                .id(100L)
                .buyer(buyer)
                .seller(seller)
                .ad(ad)
                .build();
    }

    // --- isRoomMember ---

    @Test
    @DisplayName("buyer is room member")
    void isRoomMember_buyer_true() {
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(room));
        assertTrue(chatService.isRoomMember(100L, 1L));
    }

    @Test
    @DisplayName("seller is room member")
    void isRoomMember_seller_true() {
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(room));
        assertTrue(chatService.isRoomMember(100L, 2L));
    }

    @Test
    @DisplayName("outsider is NOT room member")
    void isRoomMember_outsider_false() {
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(room));
        assertFalse(chatService.isRoomMember(100L, 99L));
    }

    @Test
    @DisplayName("non-existent room returns false")
    void isRoomMember_noRoom_false() {
        when(chatRoomRepository.findById(999L)).thenReturn(Optional.empty());
        assertFalse(chatService.isRoomMember(999L, 1L));
    }

    // --- sendMessage authorization ---

    @Test
    @DisplayName("outsider cannot send message -> AccessDeniedException")
    void sendMessage_outsider_throwsAccessDenied() {
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(room));

        assertThrows(AccessDeniedException.class, () ->
                chatService.sendMessage(100L, 99L, "привет", null));
    }

    @Test
    @DisplayName("buyer can send message")
    void sendMessage_buyer_allowed() {
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(room));
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(chatMessageRepository.save(any())).thenAnswer(inv -> {
            ChatMessage m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });

        assertDoesNotThrow(() ->
                chatService.sendMessage(100L, 1L, "привет", null));
    }

    // --- getAuthorizedAttachment ---

    @Test
    @DisplayName("outsider cannot access chat attachment -> AccessDeniedException")
    void getAttachment_outsider_throwsAccessDenied() {
        ChatMessage msg = ChatMessage.builder()
                .id(1L).room(room).sender(buyer)
                .attachmentPath("/some/file.pdf").build();
        when(chatMessageRepository.findById(1L)).thenReturn(Optional.of(msg));

        assertThrows(AccessDeniedException.class, () ->
                chatService.getAuthorizedAttachment(1L, 99L));
    }

    // --- getRecipientId ---

    @Test
    @DisplayName("getRecipientId returns other party")
    void getRecipientId_returnsOther() {
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(room));

        assertEquals(2L, chatService.getRecipientId(100L, 1L));
        assertEquals(1L, chatService.getRecipientId(100L, 2L));
    }
}
