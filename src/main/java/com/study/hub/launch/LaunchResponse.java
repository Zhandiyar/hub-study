package com.study.hub.launch;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Данные для запуска приложения")
@JsonInclude(JsonInclude.Include.ALWAYS)
public record LaunchResponse(
        @Schema(description = "Уникальный идентификатор приложения", example = "insurance-app")
        String appId,

        @Schema(description = "URL WebView", example = "https://partner.example.com/app")
        String entryUrl,

        @Schema(
                description = "Партнёрская сессия; отсутствует для API_FIRST",
                nullable = true
        )
        LaunchSessionResponse session
) {
}
