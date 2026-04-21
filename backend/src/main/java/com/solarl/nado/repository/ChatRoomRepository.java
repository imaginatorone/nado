package com.solarl.nado.repository;

import com.solarl.nado.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT r FROM ChatRoom r WHERE r.buyer.id = :userId OR r.seller.id = :userId ORDER BY r.createdAt DESC")
    List<ChatRoom> findAllByUserId(@Param("userId") Long userId);

    Optional<ChatRoom> findByAdIdAndBuyerId(Long adId, Long buyerId);
}
