package com.solarl.nado.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// dev/demo: логирует raw код в stdout, не отправляет реальное SMS
@Slf4j
@Service
public class StubSmsVerificationService implements SmsVerificationService {

    @Override
    public void sendCode(String phone, String code) {
        log.info("=== STUB SMS === phone={}, code={}", phone, code);
    }
}
