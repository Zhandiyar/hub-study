package com.study.hub.application;

import com.study.hub.client.appregistry.dto.HubApplicationConfigurationResponse;
import com.study.hub.client.appregistry.dto.InterfaceConfigurationResponse;
import com.study.hub.client.appregistry.dto.MaintenanceMessagesResponse;
import com.study.hub.client.appregistry.dto.NativeSecurityConfigurationResponse;
import com.study.hub.client.appregistry.model.ApplicationStatus;
import com.study.hub.client.appregistry.model.IntegrationType;
import com.study.hub.client.appregistry.model.SessionMode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

@Schema(description = "Конфигурация партнёрского приложения для клиентского приложения")
public record ApplicationConfigurationResponse(
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
                nullable = true
        )
        IntegrationType integrationType,

        @Schema(
                description = "Способ создания сессии; не задан до настройки конфигурации",
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

        @Schema(
                description = "Финансовая конфигурация для клиентского приложения",
                nullable = true
        )
        ApplicationFinancialConfigurationResponse financialConfiguration
) {

    public static ApplicationConfigurationResponse from(
            HubApplicationConfigurationResponse source
    ) {
        Objects.requireNonNull(source, "source must not be null");
        return new ApplicationConfigurationResponse(
                source.appId(),
                source.groupId(),
                source.status(),
                source.maintenanceMessages(),
                source.version(),
                source.integrationType(),
                source.sessionMode(),
                source.interfaceConfiguration(),
                source.nativeSecurityConfiguration(),
                toFinancialConfiguration(source)
        );
    }

    private static ApplicationFinancialConfigurationResponse toFinancialConfiguration(
            HubApplicationConfigurationResponse source
    ) {
        if (source.financialConfiguration() == null) {
            return null;
        }
        return new ApplicationFinancialConfigurationResponse(
                source.financialConfiguration().merchantId(),
                source.financialConfiguration().serviceId()
        );
    }
}
