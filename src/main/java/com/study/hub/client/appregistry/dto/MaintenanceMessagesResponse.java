package com.study.hub.client.appregistry.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Локализованные сообщения о технических работах")
public record MaintenanceMessagesResponse(
        @Schema(description = "Сообщение на русском языке")
        String ru,

        @Schema(description = "Сообщение на казахском языке")
        String kk,

        @Schema(description = "Сообщение на английском языке")
        String en
) {
}
