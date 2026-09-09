package com.study.hub.application;

import com.study.hub.client.appregistry.AppRegistryClient;
import com.study.hub.client.appregistry.dto.HubApplicationConfigurationResponse;
import com.study.hub.client.appregistry.model.ApplicationStatus;
import com.study.hub.exception.ApplicationUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationAvailabilityService {

    private final AppRegistryClient appRegistryClient;

    public HubApplicationConfigurationResponse getActiveConfiguration(String appId) {
        HubApplicationConfigurationResponse configuration =
                appRegistryClient.getApplicationConfiguration(appId);

        if (configuration.status() != ApplicationStatus.ACTIVE) {
            log.info(
                    "Partner operation rejected due to application status: appId={} status={}",
                    appId,
                    configuration.status()
            );
            throw new ApplicationUnavailableException();
        }
        return configuration;
    }
}
