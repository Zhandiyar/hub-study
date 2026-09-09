package com.study.hub.client.appregistry.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.study.hub.client.appregistry.model.CatalogItemType;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CatalogGroupResponse.class, name = "GROUP"),
        @JsonSubTypes.Type(value = CatalogApplicationResponse.class, name = "APPLICATION")
})
@Schema(
        description = "Элемент каталога",
        oneOf = {CatalogGroupResponse.class, CatalogApplicationResponse.class}
)
public sealed interface CatalogItemResponse
        permits CatalogGroupResponse, CatalogApplicationResponse {

    String id();

    CatalogItemType type();
}
