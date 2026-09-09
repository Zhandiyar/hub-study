package com.study.hub.client.appregistry.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Настройки нативной безопасности")
public record NativeSecurityConfigurationResponse(
        @Schema(description = "Домены, которым разрешено взаимодействие с приложением")
        List<String> allowedDomains,

        @Schema(description = "Политика Content Security Policy")
        String contentSecurityPolicy,

        @Schema(description = "Разрешённые методы нативного bridge")
        List<String> allowedBridgeMethods
) {
}
