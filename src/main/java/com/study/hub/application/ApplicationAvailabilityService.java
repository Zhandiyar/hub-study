package com.study.hub.application;

import com.study.hub.client.appregistry.AppRegistryClient;
import com.study.hub.client.appregistry.dto.HubApplicationConfigurationResponse;
import com.study.hub.client.appregistry.dto.ApplicationRoutingResponse;
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

    public ApplicationRoutingResponse getActiveRoutingByAppId(String appId) {
        ApplicationRoutingResponse routing = appRegistryClient.getRoutingByAppId(appId);

        if (routing.status() != ApplicationStatus.ACTIVE) {
            log.info(
                    "Partner operation rejected due to application status: "
                            + "appId={} status={}",
                    routing.appId(),
                    routing.status()
            );
            throw new ApplicationUnavailableException();
        }
        if (routing.serviceId() == null
                || routing.serviceId() <= 0
                || routing.contractNumberServiceFieldId() == null
                || routing.contractNumberServiceFieldId() <= 0) {
            log.info(
                    "Partner operation rejected because payment routing is not configured: "
                            + "appId={}",
                    routing.appId()
            );
            throw new ApplicationUnavailableException();
        }
        return routing;
    }
}
