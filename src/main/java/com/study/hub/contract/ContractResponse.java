package com.study.hub.contract;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Данные договора и готовые реквизиты платежа")
public record ContractResponse(
        @Schema(description = "Номер договора у партнёра")
        String contractNumber,

        @Schema(description = "Статус договора у партнёра")
        String status,

        @Schema(description = "Сумма к оплате")
        BigDecimal amount,

        @Schema(description = "Код валюты ISO 4217")
        String currency,

        @Schema(description = "Реквизиты, которые мобильный клиент передаёт в платёж")
        List<PaymentDetailResponse> paymentDetails
) {
    public ContractResponse {
        paymentDetails = List.copyOf(paymentDetails);
    }
}
