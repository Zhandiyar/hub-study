package com.study.hub.client.appregistry.dto;

import com.study.hub.client.appregistry.model.ApplicationStatus;

import java.util.List;

public record ApplicationRoutingResponse(
        String appId,
        ApplicationStatus status,
        Long serviceId,
        Long contractNumberServiceFieldId,
        List<PaymentFieldMappingResponse> paymentFieldMappings
) {
}
