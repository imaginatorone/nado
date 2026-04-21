package com.solarl.nado.repository;

import com.solarl.nado.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndIsReadFalse(Long userId);

    List<Notification> findByUserIdAndIsReadFalse(Long userId);

    // dedup: проверить существование уведомления с таким же type+payload за последний час
    boolean existsByUserIdAndTypeAndPayload(Long userId, Notification.NotificationType type, String payload);
}
