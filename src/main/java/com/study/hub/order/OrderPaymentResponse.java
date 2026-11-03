package com.study.hub.order;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Данные для создания платежа мобильным клиентом")
public record OrderPaymentResponse(
        @Schema(description = "Идентификатор платёжного сервиса", example = "191")
        long serviceId,

        @Schema(description = "Реквизиты платёжной услуги")
        List<PaymentDetailResponse> details
) {
    public OrderPaymentResponse {
        details = List.copyOf(details);
    }
}
