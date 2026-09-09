package com.study.hub.application;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Финансовая конфигурация, доступная клиентскому приложению")
public record ApplicationFinancialConfigurationResponse(
        @Schema(description = "Идентификатор мерчанта")
        String merchantId,

        @Schema(description = "Идентификатор платёжного сервиса")
        String serviceId
) {
}
