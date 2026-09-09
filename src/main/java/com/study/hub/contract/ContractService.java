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

    public JsonNode getContract(String appId, String contractNumber) {
        applicationAvailabilityService.getActiveConfiguration(appId);
        return partnerApiClient.getContract(appId, contractNumber);
    }
}
