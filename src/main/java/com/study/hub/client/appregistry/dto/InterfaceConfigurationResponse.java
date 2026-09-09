package com.study.hub.client.appregistry.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Настройки пользовательского интерфейса")
public record InterfaceConfigurationResponse(
        @Schema(description = "HTTPS-адрес пользовательского интерфейса партнёра")
        String entryUrl,

        @Schema(description = "Локализованный заголовок приложения")
        LocalizedTextResponse title,

        @Schema(description = "URL иконки приложения", nullable = true)
        String iconUrl,

        @Schema(description = "Позиция приложения в группе")
        int displayOrder
) {
}
