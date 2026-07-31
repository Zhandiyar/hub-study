package com.study.hub.partnerapp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Группа партнёрских приложений")
public record PartnerApplicationGroupResponse(
        @Schema(description = "Уникальный идентификатор группы", example = "insurance")
        String groupId,

        @Schema(description = "Отображаемое название группы", example = "Страхование")
        String displayName,

        @Schema(description = "Версия группы", example = "1")
        long version,

        @Schema(description = "Приложения текущей страницы")
        List<PartnerApplicationResponse> applications,

        @Schema(description = "Номер страницы, начиная с нуля", example = "0")
        int page,

        @Schema(description = "Размер страницы", example = "20")
        int size,

        @Schema(description = "Общее количество приложений", example = "2")
        long totalElements,

        @Schema(description = "Общее количество страниц", example = "1")
        int totalPages
) {
}
