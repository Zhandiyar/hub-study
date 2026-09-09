package com.study.hub.openapi;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(name = "ProblemDetail", description = "Описание ошибки в формате RFC 9457")
public record ApiProblemDetail(
        @Schema(description = "URI типа ошибки", format = "uri")
        String type,

        @Schema(description = "Краткое название ошибки")
        String title,

        @Schema(description = "HTTP-статус")
        Integer status,

        @Schema(description = "Описание ошибки")
        String detail,

        @Schema(description = "URI запроса, в котором произошла ошибка", format = "uri")
        String instance,

        @Schema(description = "Стабильный машинный код ошибки")
        String code,

        @Schema(
                description = "Бизнес-код ошибки партнёра для обработки на frontend",
                example = "CONTRACT_DIFFERENT_AMOUNT",
                nullable = true
        )
        String errorCode,

        @Schema(description = "Ошибки валидации по полям")
        Map<String, List<String>> errors,

        @Schema(description = "Идентификатор трассировки")
        String traceId,

        @Schema(description = "Идентификатор бизнес-запроса")
        String correlationId
) {
}
