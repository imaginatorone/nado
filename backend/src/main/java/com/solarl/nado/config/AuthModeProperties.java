package com.solarl.nado.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * режим: "legacy" (самописный JWT, переходный) | "keycloak" (целевой).
 */
@Data
@ConfigurationProperties(prefix = "app.auth")
public class AuthModeProperties {

    private String mode = "legacy";

    public boolean isKeycloak() {
        return "keycloak".equalsIgnoreCase(mode);
    }

    public boolean isLegacy() {
        return "legacy".equalsIgnoreCase(mode);
    }
}
