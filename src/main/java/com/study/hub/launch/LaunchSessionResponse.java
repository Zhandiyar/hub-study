package com.study.hub.launch;

import com.study.hub.client.appregistry.model.SessionMode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

@Schema(description = "Данные партнёрской сессии")
public record LaunchSessionResponse(
        @Schema(
                description = "Тип значения партнёрской сессии",
                allowableValues = {"TOKEN", "CODE"},
                example = "TOKEN"
        )
        SessionMode type,

        @Schema(description = "Токен или одноразовый код, полученный от партнёра")
        String value
) {

    public LaunchSessionResponse {
        Objects.requireNonNull(type, "type must not be null");
        if (type == SessionMode.NONE) {
            throw new IllegalArgumentException("NONE is not a partner session type");
        }
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }
}
