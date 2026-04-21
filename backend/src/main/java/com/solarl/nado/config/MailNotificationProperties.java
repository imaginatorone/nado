package com.solarl.nado.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.mail")
public class MailNotificationProperties {
    private boolean enabled = false;
    private String from = "noreply@nado.ru";
}
