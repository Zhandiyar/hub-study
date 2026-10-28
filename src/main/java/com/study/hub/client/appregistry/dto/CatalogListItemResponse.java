package com.study.hub.client.appregistry.dto;

import com.study.hub.client.appregistry.model.CatalogItemType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Элемент плоского списка каталога")
public record CatalogListItemResponse(
        @Schema(description = "groupId для группы или appId для приложения")
        String id,

        @Schema(description = "Тип элемента", example = "APPLICATION")
        CatalogItemType type,

        @Schema(description = "Локализованный заголовок")
        LocalizedTextResponse title,

        @Schema(description = "URL иконки", nullable = true)
        String iconUrl
) {
}
