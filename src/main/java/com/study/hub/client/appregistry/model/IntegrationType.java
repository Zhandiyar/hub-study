package com.study.hub.client.appregistry.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Тип интеграции с приложением")
public enum IntegrationType {
    PARTNER_WEBVIEW,
    API_FIRST
}
