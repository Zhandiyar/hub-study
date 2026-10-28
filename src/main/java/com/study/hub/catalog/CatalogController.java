package com.study.hub.catalog;

import com.study.hub.client.appregistry.AppRegistryClient;
import com.study.hub.client.appregistry.dto.CatalogGroupResponse;
import com.study.hub.client.appregistry.dto.CatalogListItemResponse;
import com.study.hub.openapi.ApiProblemResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/hub/api/v1/catalog")
@RequiredArgsConstructor
@Tag(
        name = "Каталог",
        description = "Навигационный каталог партнёрских приложений"
)
@ApiProblemResponses
public class CatalogController {

    private final AppRegistryClient appRegistryClient;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Получить каталог партнёрских приложений")
    @ApiResponse(responseCode = "200", description = "Дерево групп и приложений")
    public CatalogGroupResponse getCatalog() {
        return appRegistryClient.getCatalog();
    }

    @GetMapping(
            value = "/groups/{groupId}/items",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Получить непосредственные элементы группы плоским списком")
    @ApiResponse(responseCode = "200", description = "Группы и приложения без вложенности")
    public List<CatalogListItemResponse> getGroupItems(
            @PathVariable String groupId
    ) {
        return appRegistryClient.getCatalogGroupItems(groupId);
    }
}
