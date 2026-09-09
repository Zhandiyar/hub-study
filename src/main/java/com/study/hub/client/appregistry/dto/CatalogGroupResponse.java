package com.study.hub.client.appregistry.dto;

import com.study.hub.client.appregistry.model.CatalogItemType;
import com.study.hub.client.appregistry.model.GroupLayout;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Группа каталога")
public record CatalogGroupResponse(
        @Schema(
                description = "Идентификатор элемента; для GROUP содержит groupId",
                example = "insurance"
        )
        String id,

        @Schema(description = "Тип элемента", example = "GROUP")
        CatalogItemType type,

        @Schema(description = "Способ отображения дочерних элементов")
        GroupLayout layout,

        @Schema(description = "Локализованный заголовок группы")
        LocalizedTextResponse title,

        @Schema(description = "URL иконки группы", nullable = true)
        String iconUrl,

        @Schema(description = "Дочерние группы и приложения")
        List<CatalogItemResponse> items
) implements CatalogItemResponse {

    public CatalogGroupResponse(
            String id,
            GroupLayout layout,
            LocalizedTextResponse title,
            String iconUrl,
            List<CatalogItemResponse> items
    ) {
        this(id, CatalogItemType.GROUP, layout, title, iconUrl, items);
    }

    public CatalogGroupResponse {
        type = CatalogItemType.GROUP;
        items = List.copyOf(items);
    }
}
