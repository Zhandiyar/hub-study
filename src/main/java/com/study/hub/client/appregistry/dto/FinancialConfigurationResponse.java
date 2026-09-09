package com.study.hub.client.appregistry.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Финансовая и бизнес-конфигурация")
public record FinancialConfigurationResponse(
        @Schema(description = "Идентификатор мерчанта")
        String merchantId,

        @Schema(description = "Идентификатор платёжного сервиса")
        String serviceId,

        @Schema(description = "HTTPS-адрес webhook для уведомлений")
        String paymentWebhookUrl,

        @Schema(description = "Непрозрачный идентификатор учётных данных")
        String credentialId
) {
}
