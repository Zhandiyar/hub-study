package com.study.hub.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.study.hub.application.ApplicationAvailabilityService;
import com.study.hub.client.partner.PartnerApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ApplicationAvailabilityService applicationAvailabilityService;
    private final PartnerApiClient partnerApiClient;

    public JsonNode getContract(String serviceId, String contractNumber) {
        var routing = applicationAvailabilityService
                .getActiveRoutingByServiceId(serviceId);
        return partnerApiClient.getContract(routing.appId(), contractNumber);
    }
}
