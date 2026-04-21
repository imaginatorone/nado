package com.solarl.nado.repository;

import com.solarl.nado.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByRoomIdOrderByCreatedAtAsc(Long roomId);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.room.id = :roomId AND m.sender.id <> :userId AND m.read = false")
    long countUnreadInRoom(@Param("roomId") Long roomId, @Param("userId") Long userId);

    @Query("SELECT COUNT(m) FROM ChatMessage m JOIN m.room r WHERE (r.buyer.id = :userId OR r.seller.id = :userId) AND m.sender.id <> :userId AND m.read = false")
    long countTotalUnread(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.read = true WHERE m.room.id = :roomId AND m.sender.id <> :userId AND m.read = false")
    void markAsRead(@Param("roomId") Long roomId, @Param("userId") Long userId);
}
