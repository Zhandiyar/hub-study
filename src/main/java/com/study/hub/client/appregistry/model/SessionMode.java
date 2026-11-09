package com.study.hub.client.appregistry.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Способ подготовки запуска партнёрского приложения")
public enum SessionMode {
    TOKEN,
    CODE,
    VIEW_URL,
    NONE
}
