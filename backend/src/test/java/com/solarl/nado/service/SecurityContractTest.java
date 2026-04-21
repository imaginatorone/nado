package com.solarl.nado.service;

import com.solarl.nado.dto.response.AdResponse;
import com.solarl.nado.dto.response.SellerProfileResponse;
import com.solarl.nado.dto.response.UserPublicResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Проверка security-контрактов DTO — приватные данные не утекают в публичные DTO.
 */
class SecurityContractTest {

    @Test
    @DisplayName("AdResponse не содержит полей userEmail и userPhone")
    void adResponse_doesNotContainSensitiveFields() {
        List<String> fields = getFieldNames(AdResponse.class);
        assertFalse(fields.contains("userEmail"), "AdResponse не должен содержать userEmail");
        assertFalse(fields.contains("userPhone"), "AdResponse не должен содержать userPhone");
    }

    @Test
    @DisplayName("SellerProfileResponse не содержит полей email и phone")
    void sellerProfileResponse_doesNotContainSensitiveFields() {
        List<String> fields = getFieldNames(SellerProfileResponse.class);
        assertFalse(fields.contains("email"), "SellerProfileResponse не должен содержать email");
        assertFalse(fields.contains("phone"), "SellerProfileResponse не должен содержать phone");
    }

    @Test
    @DisplayName("UserPublicResponse не содержит полей email и phone")
    void userPublicResponse_doesNotContainSensitiveFields() {
        List<String> fields = getFieldNames(UserPublicResponse.class);
        assertFalse(fields.contains("email"), "UserPublicResponse не должен содержать email");
        assertFalse(fields.contains("phone"), "UserPublicResponse не должен содержать phone");
    }

    @Test
    @DisplayName("NotificationResponse не содержит полей userEmail и password")
    void notificationResponse_doesNotContainSensitiveFields() {
        List<String> fields = getFieldNames(com.solarl.nado.dto.response.NotificationResponse.class);
        assertFalse(fields.contains("userEmail"), "NotificationResponse не должен содержать userEmail");
        assertFalse(fields.contains("password"), "NotificationResponse не должен содержать password");
        assertFalse(fields.contains("userPhone"), "NotificationResponse не должен содержать userPhone");
    }

    @Test
    @DisplayName("AuctionResponse не содержит полей sellerEmail и sellerPhone")
    void auctionResponse_doesNotContainSensitiveFields() {
        List<String> fields = getFieldNames(com.solarl.nado.dto.response.AuctionResponse.class);
        assertFalse(fields.contains("sellerEmail"), "AuctionResponse не должен содержать sellerEmail");
        assertFalse(fields.contains("sellerPhone"), "AuctionResponse не должен содержать sellerPhone");
    }

    private List<String> getFieldNames(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toList());
    }
}
