package com.solarl.nado.service;

import com.solarl.nado.entity.PhoneVerification;
import com.solarl.nado.entity.User;
import com.solarl.nado.repository.PhoneVerificationRepository;
import com.solarl.nado.repository.UserRepository;
import com.solarl.nado.security.AuthFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

    private static final int CODE_LENGTH = 6;
    private static final int TTL_MINUTES = 5;
    private static final int COOLDOWN_SECONDS = 60;
    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_REQUESTS_PER_HOUR = 5;

    private final PhoneVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final SmsVerificationService smsSender;
    private final PasswordEncoder passwordEncoder;
    private final AuthFacade authFacade;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public void requestCode(String phone) {
        Long userId = authFacade.getCurrentUserId();
        phone = normalizePhone(phone);

        // rate limiting: не более MAX_REQUESTS_PER_HOUR запросов в час
        long recentCount = verificationRepository.countByUserIdAndCreatedAtAfter(
                userId, LocalDateTime.now().minusHours(1));
        if (recentCount >= MAX_REQUESTS_PER_HOUR) {
            throw new IllegalStateException("слишком много запросов, попробуйте позже");
        }

        // cooldown: не чаще раза в минуту
        var existing = verificationRepository
                .findTopByUserIdAndPhoneAndVerifiedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        userId, phone, LocalDateTime.now());
        if (existing.isPresent()) {
            LocalDateTime cooldownEnd = existing.get().getCreatedAt().plusSeconds(COOLDOWN_SECONDS);
            if (LocalDateTime.now().isBefore(cooldownEnd)) {
                throw new IllegalStateException("повторный запрос возможен через минуту");
            }
        }

        String code = generateCode();

        PhoneVerification verification = PhoneVerification.builder()
                .userId(userId)
                .phone(phone)
                .codeHash(passwordEncoder.encode(code))
                .expiresAt(LocalDateTime.now().plusMinutes(TTL_MINUTES))
                .build();
        verificationRepository.save(verification);

        smsSender.sendCode(phone, code);
        log.info("OTP запрошен: userId={}, phone={}***", userId, phone.substring(0, Math.min(4, phone.length())));
    }

    @Transactional
    public boolean verifyCode(String phone, String code) {
        Long userId = authFacade.getCurrentUserId();
        phone = normalizePhone(phone);

        var optVerification = verificationRepository
                .findTopByUserIdAndPhoneAndVerifiedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        userId, phone, LocalDateTime.now());

        if (optVerification.isEmpty()) {
            throw new IllegalStateException("код не найден или истёк");
        }

        PhoneVerification verification = optVerification.get();

        if (verification.getAttempts() >= MAX_ATTEMPTS) {
            throw new IllegalStateException("превышено количество попыток, запросите новый код");
        }

        verification.setAttempts(verification.getAttempts() + 1);

        if (!passwordEncoder.matches(code, verification.getCodeHash())) {
            verificationRepository.save(verification);
            return false;
        }

        // успешная верификация
        verification.setVerified(true);
        verificationRepository.save(verification);

        User user = userRepository.findById(userId).orElseThrow();
        user.setPhone(phone);
        user.setPhoneVerified(true);
        user.setPhoneVerifiedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("телефон верифицирован: userId={}", userId);
        return true;
    }

    // при смене номера сбрасываем phoneVerified
    @Transactional
    public void onPhoneChanged(User user, String newPhone) {
        String normalized = normalizePhone(newPhone);
        if (normalized.equals(normalizePhone(user.getPhone()))) return;

        user.setPhone(normalized);
        user.setPhoneVerified(false);
        user.setPhoneVerifiedAt(null);
        userRepository.save(user);
    }

    private String generateCode() {
        int code = random.nextInt((int) Math.pow(10, CODE_LENGTH));
        return String.format("%0" + CODE_LENGTH + "d", code);
    }

    private String normalizePhone(String phone) {
        if (phone == null) throw new IllegalArgumentException("номер телефона обязателен");
        return phone.replaceAll("[^0-9+]", "");
    }
}
