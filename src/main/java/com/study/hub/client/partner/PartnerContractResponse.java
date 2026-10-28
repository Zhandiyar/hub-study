package com.study.hub.client.partner;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Общий внутренний контракт adapter → Hub. */
public record PartnerContractResponse(
        String contractNumber,
        String status,
        BigDecimal amount,
        String currency,
        Map<String, String> paymentFields
) {
    public PartnerContractResponse {
        paymentFields = paymentFields == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(paymentFields));
    }
}
