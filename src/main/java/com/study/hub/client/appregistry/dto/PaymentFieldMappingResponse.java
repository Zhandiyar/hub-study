package com.study.hub.client.appregistry.dto;

/** Стабильный код реквизита адаптера и его поле в платёжной платформе. */
public record PaymentFieldMappingResponse(
        String code,
        Long serviceFieldId
) {
}
