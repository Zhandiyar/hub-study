package com.study.hub.client.appregistry;

import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties("app.clients.app-registry")
public record AppRegistryClientProperties(
        @NotNull
        URI baseUrl,

        @NotNull
        @DurationMin(millis = 1)
        Duration connectTimeout,

        @NotNull
        @DurationMin(millis = 1)
        Duration readTimeout
) {
}
