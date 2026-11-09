package com.study.hub.client.appregistry.dto;

import com.study.hub.client.appregistry.model.ApplicationStatus;
import com.study.hub.client.appregistry.model.IntegrationType;
import com.study.hub.client.appregistry.model.SessionMode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Конфигурация партнёрского приложения для использования Hub")
public record HubApplicationConfigurationResponse(
        @Schema(description = "Уникальный идентификатор приложения", example = "partner-app")
        String appId,

        @Schema(description = "Идентификатор группы приложений", example = "insurance")
        String groupId,

        @Schema(description = "Текущий статус приложения")
        ApplicationStatus status,

        @Schema(
                description = "Сообщения о технических работах на трёх языках",
                nullable = true
        )
        MaintenanceMessagesResponse maintenanceMessages,

        @Schema(description = "Версия конфигурации", example = "3")
        long version,

        @Schema(
                description = "Тип интеграции; не задан до настройки конфигурации",
                example = "PARTNER_WEBVIEW",
                nullable = true
        )
        IntegrationType integrationType,

        @Schema(
                description = "Способ подготовки запуска; не задан до настройки конфигурации",
                example = "TOKEN",
                nullable = true
        )
        SessionMode sessionMode,

        @Schema(
                description = "Настройки пользовательского интерфейса; null до настройки",
                nullable = true
        )
        InterfaceConfigurationResponse interfaceConfiguration,

        @Schema(
                description = "Настройки нативной безопасности; null до настройки",
                nullable = true
        )
        NativeSecurityConfigurationResponse nativeSecurityConfiguration,

        @Schema(description = "Финансовая и бизнес-конфигурация", nullable = true)
        FinancialConfigurationResponse financialConfiguration
) {
}
