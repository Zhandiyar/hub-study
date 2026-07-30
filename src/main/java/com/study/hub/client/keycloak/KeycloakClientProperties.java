package com.study.hub.client.keycloak;

import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties("app.clients.keycloak")
public record KeycloakClientProperties(
        @NotNull
        @DurationMin(millis = 1)
        Duration connectTimeout,

        @NotNull
        @DurationMin(millis = 1)
        Duration readTimeout
) {
}
