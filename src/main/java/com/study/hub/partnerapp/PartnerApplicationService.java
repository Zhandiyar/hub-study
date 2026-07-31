package com.study.hub.partnerapp;

import com.study.hub.client.appregistry.AppRegistryClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PartnerApplicationService {

    private final AppRegistryClient appRegistryClient;
    private final PartnerApplicationMapper mapper;

    public PartnerApplicationResponse getApplication(String appId) {
        return mapper.toResponse(appRegistryClient.getApplicationConfiguration(appId));
    }
}
