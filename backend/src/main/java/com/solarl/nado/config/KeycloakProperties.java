package com.solarl.nado.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.keycloak")
public class KeycloakProperties {

    private String issuerUri = "http://localhost:8180/realms/nado";
    private String jwkSetUri = "http://localhost:8180/realms/nado/protocol/openid-connect/certs";
    private String publicUrl = "http://localhost:8180";
    private String clientId = "nado-frontend";
}
