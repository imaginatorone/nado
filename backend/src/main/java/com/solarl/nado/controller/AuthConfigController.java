package com.solarl.nado.controller;

import com.solarl.nado.config.AuthModeProperties;
import com.solarl.nado.config.KeycloakProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * frontend получает отсюда режим auth, чтобы не хардкодить стратегию логина.
 */
@RestController
@RequiredArgsConstructor
public class AuthConfigController {

    private final AuthModeProperties authModeProperties;
    private final KeycloakProperties keycloakProperties;

    @GetMapping("/auth-config")
    public ResponseEntity<Map<String, Object>> getAuthConfig() {
        if (authModeProperties.isKeycloak()) {
            return ResponseEntity.ok(Map.of(
                    "mode", "keycloak",
                    "keycloakUrl", keycloakProperties.getPublicUrl(),
                    "realm", "nado",
                    "clientId", keycloakProperties.getClientId()
            ));
        }
        return ResponseEntity.ok(Map.of("mode", "legacy"));
    }
}
