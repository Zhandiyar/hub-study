package com.study.hub.client.appregistry.dto;

import com.study.hub.client.appregistry.model.ApplicationStatus;
import com.study.hub.client.appregistry.model.CatalogItemType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Партнёрское приложение в каталоге")
public record CatalogApplicationResponse(
        @Schema(
                description = "Идентификатор элемента; для APPLICATION содержит appId",
                example = "insurance-app"
        )
        String id,

        @Schema(description = "Тип элемента", example = "APPLICATION")
        CatalogItemType type,

        @Schema(description = "Локализованный заголовок приложения")
        LocalizedTextResponse title,

        @Schema(description = "URL иконки приложения", nullable = true)
        String iconUrl,

        @Schema(description = "Текущий статус приложения")
        ApplicationStatus status,

        @Schema(description = "Сообщения о технических работах", nullable = true)
        MaintenanceMessagesResponse maintenanceMessages
) implements CatalogItemResponse {

    public CatalogApplicationResponse(
            String id,
            LocalizedTextResponse title,
            String iconUrl,
            ApplicationStatus status,
            MaintenanceMessagesResponse maintenanceMessages
    ) {
        this(
                id,
                CatalogItemType.APPLICATION,
                title,
                iconUrl,
                status,
                maintenanceMessages
        );
    }

    public CatalogApplicationResponse {
        type = CatalogItemType.APPLICATION;
    }
}
