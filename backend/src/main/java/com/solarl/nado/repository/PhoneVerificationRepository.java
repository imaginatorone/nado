package com.solarl.nado.repository;

import com.solarl.nado.entity.PhoneVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, Long> {

    Optional<PhoneVerification> findTopByUserIdAndPhoneAndVerifiedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            Long userId, String phone, LocalDateTime now);

    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime after);
}
