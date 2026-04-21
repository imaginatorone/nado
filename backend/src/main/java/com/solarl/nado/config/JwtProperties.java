package com.solarl.nado.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки JWT-аутентификации.
 * Значения берутся из application.yml (prefix: app.jwt).
 */
@Data
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret;
    private long expirationMs;
}
