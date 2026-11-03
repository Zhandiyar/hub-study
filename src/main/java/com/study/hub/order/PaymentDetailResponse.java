package com.study.hub.order;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Готовый реквизит для создания платежа")
public record PaymentDetailResponse(
        @Schema(description = "Идентификатор поля платёжной услуги", example = "901")
        long serviceFieldId,

        @Schema(description = "Значение реквизита", example = "TRAVEL-2026-001")
        String value
) {
}
