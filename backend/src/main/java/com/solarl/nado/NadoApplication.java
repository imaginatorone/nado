package com.solarl.nado;

import com.solarl.nado.config.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        JwtProperties.class,
        StorageProperties.class,
        AdminProperties.class,
        KeycloakProperties.class,
        AuthModeProperties.class
})
public class NadoApplication {
    public static void main(String[] args) {
        SpringApplication.run(NadoApplication.class, args);
    }
}
