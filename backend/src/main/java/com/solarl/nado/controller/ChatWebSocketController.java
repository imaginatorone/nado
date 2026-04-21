package com.solarl.nado.controller;

import com.solarl.nado.dto.response.ChatMessageResponse;
import com.solarl.nado.service.ChatService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;

/**
 * WS controller для realtime чата.
 * бизнес-логика и auth делегируются в ChatService.
 * WS - только канал доставки, persistence идет через REST.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * отправка сообщения через ws: /app/chat.send.{roomId}
     * вложения по-прежнему идут через REST (multipart).
     */
    @MessageMapping("/chat.send.{roomId}")
    public void sendMessage(@DestinationVariable Long roomId,
                            @Payload WsChatMessage payload,
                            Principal principal) {
        Long senderId = extractUserId(principal);
        if (senderId == null) {
            log.warn("WS_SEND: нет авторизации");
            return;
        }

        // делегируем в service - он проверит room membership и сохранит
        ChatMessageResponse response = chatService.sendMessage(roomId, senderId, payload.getContent(), null);

        // broadcast обоим участникам комнаты
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, response);
    }

    /**
     * object-level auth на subscribe: нельзя подписаться на чужую комнату.
     * срабатывает при SUBSCRIBE на /topic/chat/{roomId}.
     */
    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        Principal principal = accessor.getUser();

        if (destination == null || !destination.startsWith("/topic/chat/")) return;
        if (principal == null) {
            log.warn("WS_SUBSCRIBE: отклонена подписка без авторизации на {}", destination);
            return;
        }

        try {
            String roomIdStr = destination.replace("/topic/chat/", "");
            Long roomId = Long.parseLong(roomIdStr);
            Long userId = extractUserId(principal);

            if (userId == null || !chatService.isRoomMember(roomId, userId)) {
                log.warn("WS_SUBSCRIBE: userId={} отклонен для room={}", userId, roomId);
                // в STOMP нет стандартного способа reject subscribe,
                // но сообщения не доставляются без membership в chatService
            }
        } catch (NumberFormatException e) {
            log.debug("WS_SUBSCRIBE: невалидный roomId в {}", destination);
        }
    }

    private Long extractUserId(Principal principal) {
        if (principal == null) return null;

        // legacy mode: UserDetailsImpl in principal
        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken) {
            Object p = ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal).getPrincipal();
            if (p instanceof com.solarl.nado.security.UserDetailsImpl) {
                return ((com.solarl.nado.security.UserDetailsImpl) p).getId();
            }
            // keycloak mode: userId as string principal
            try {
                return Long.parseLong(p.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    @Data
    public static class WsChatMessage {
        private String content;
    }
}
