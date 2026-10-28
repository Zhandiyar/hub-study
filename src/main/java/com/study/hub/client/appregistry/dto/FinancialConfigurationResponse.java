package com.study.hub.client.appregistry.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Финансовая и бизнес-конфигурация")
public record FinancialConfigurationResponse(
        @Schema(description = "Идентификатор мерчанта")
        String merchantId,

        @Schema(description = "Идентификатор платёжного сервиса")
        Long serviceId,

        @Schema(description = "Идентификатор поля номера договора")
        Long contractNumberServiceFieldId,

        @Schema(description = "Дополнительные поля платежа")
        List<PaymentFieldMappingResponse> notificationFields,

        @Schema(description = "HTTPS-адрес webhook для уведомлений")
        String paymentWebhookUrl,

        @Schema(description = "Непрозрачный идентификатор учётных данных")
        String credentialId
) {
}
