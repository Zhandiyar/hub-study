package com.study.hub.launch;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.study.hub.client.appregistry.model.SessionMode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

@Schema(description = "Данные партнёрской сессии")
public record LaunchSessionResponse(
        @Schema(
                description = "Тип значения партнёрской сессии",
                allowableValues = {"TOKEN", "CODE", "VIEW_URL"},
                example = "TOKEN"
        )
        SessionMode type,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(
                description = "Токен или одноразовый код; отсутствует для VIEW_URL",
                nullable = true
        )
        String value
) {

    public LaunchSessionResponse {
        Objects.requireNonNull(type, "type must not be null");
        if (type == SessionMode.NONE) {
            throw new IllegalArgumentException("NONE is not a partner session type");
        }
        if (type == SessionMode.VIEW_URL) {
            if (value != null) {
                throw new IllegalArgumentException("VIEW_URL must not contain a session value");
            }
        } else {
            Objects.requireNonNull(value, "value must not be null");
            if (value.isBlank()) {
                throw new IllegalArgumentException("value must not be blank");
            }
        }
    }
}
