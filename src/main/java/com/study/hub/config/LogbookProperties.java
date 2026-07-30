package com.study.hub.config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

@Validated
@ConfigurationProperties(prefix = "logbook.masking")
public record LogbookProperties(@NotEmpty Set<String> sensitiveFields) {
}
