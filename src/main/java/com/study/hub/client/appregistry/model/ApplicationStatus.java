package com.study.hub.client.appregistry.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Статус партнёрского приложения")
public enum ApplicationStatus {
    ACTIVE,
    DISABLED,
    MAINTENANCE
}
