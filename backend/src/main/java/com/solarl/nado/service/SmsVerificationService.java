package com.solarl.nado.service;

// провайдер отправки SMS-кода
public interface SmsVerificationService {
    void sendCode(String phone, String code);
}
