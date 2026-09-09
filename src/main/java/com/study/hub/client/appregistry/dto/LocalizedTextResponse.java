package com.study.hub.client.appregistry.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Локализованный текст")
public record LocalizedTextResponse(
        @Schema(description = "Текст на русском языке") String ru,
        @Schema(description = "Текст на казахском языке") String kk,
        @Schema(description = "Текст на английском языке") String en
) {
}
