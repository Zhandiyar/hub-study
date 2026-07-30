package com.study.hub.partnerapp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Безопасная клиентская конфигурация партнёрского приложения")
public record PartnerApplicationResponse(
        @Schema(description = "Уникальный идентификатор приложения", example = "partner-app")
        String appId,

        @Schema(description = "Идентификатор группы приложений", example = "insurance")
        String groupId,

        @Schema(description = "Версия конфигурации", example = "3")
        long version,

        @Schema(description = "Способ создания партнёрской сессии")
        SessionMode sessionMode,

        @Schema(description = "Настройки пользовательского интерфейса")
        InterfaceConfiguration interfaceConfiguration,

        @Schema(description = "Настройки нативной безопасности")
        NativeSecurityConfiguration nativeSecurityConfiguration
) {

    public record InterfaceConfiguration(
            @Schema(description = "HTTPS-адрес пользовательского интерфейса партнёра")
            String entryUrl,

            @Schema(description = "Заголовок приложения")
            String title
    ) {
    }

    public record NativeSecurityConfiguration(
            @Schema(description = "Домены, которым разрешено взаимодействие с приложением")
            List<String> allowedDomains,

            @Schema(description = "Политика Content Security Policy")
            String contentSecurityPolicy,

            @Schema(description = "Разрешённые методы нативного bridge")
            List<String> allowedBridgeMethods
    ) {
    }
}
