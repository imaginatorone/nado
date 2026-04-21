package com.solarl.nado.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки начального администратора.
 * Создание происходит только при seedEnabled=true (по умолчанию — false).
 */
@Data
@ConfigurationProperties(prefix = "app.admin")
public class AdminProperties {
    private String email = "admin@nado.ru";
    private String password = "N@d0_Adm!n_2026";

    /** Разрешает создание дефолтного администратора при старте (только для dev) */
    private boolean seedEnabled = false;
}
