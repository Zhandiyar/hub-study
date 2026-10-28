package com.study.hub.client.appregistry.dto;

import com.study.hub.client.appregistry.model.ApplicationStatus;

import java.util.List;

public record ApplicationRoutingResponse(
        Long serviceId,
        String groupId,
        String appId,
        ApplicationStatus status,
        Long contractNumberServiceFieldId,
        List<PaymentFieldMappingResponse> paymentFieldMappings
) {
}
