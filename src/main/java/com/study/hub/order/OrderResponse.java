package com.study.hub.order;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Schema(description = "Данные заказа и готовые реквизиты платежа")
public record OrderResponse(
        @Schema(description = "Идентификатор заказа у партнёра")
        String orderId,

        @Schema(description = "Статус заказа у партнёра")
        String status,

        @Schema(description = "Сумма к оплате")
        BigDecimal amount,

        @Schema(description = "Данные заказа в формате конкретного приложения")
        Map<String, Object> data,

        @Schema(description = "Данные, необходимые мобильному клиенту для создания платежа")
        OrderPaymentResponse payment
) {
    public OrderResponse {
        data = data == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(data));
    }
}
