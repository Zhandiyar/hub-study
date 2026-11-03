package com.study.hub.client.partner;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Общий внутренний контракт adapter → Hub. */
public record PartnerOrderResponse(
        String orderId,
        String status,
        BigDecimal amount,
        Map<String, Object> data,
        Map<String, String> paymentFields
) {
    public PartnerOrderResponse {
        data = data == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(data));
        paymentFields = paymentFields == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(paymentFields));
    }
}
