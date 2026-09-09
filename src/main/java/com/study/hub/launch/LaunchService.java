package com.study.hub.launch;

import com.study.hub.application.ApplicationAvailabilityService;
import com.study.hub.client.appregistry.dto.HubApplicationConfigurationResponse;
import com.study.hub.client.appregistry.model.IntegrationType;
import com.study.hub.client.appregistry.model.SessionMode;
import com.study.hub.client.partner.PartnerApiClient;
import com.study.hub.exception.AppRegistryUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LaunchService {

    private final ApplicationAvailabilityService applicationAvailabilityService;
    private final PartnerApiClient partnerApiClient;

    public LaunchResponse launch(String appId) {
        HubApplicationConfigurationResponse configuration =
                applicationAvailabilityService.getActiveConfiguration(appId);

        validateLaunchConfiguration(appId, configuration);
        LaunchSessionResponse session = createSession(appId, configuration);

        log.info(
                "Application launch prepared: appId={} integrationType={} sessionMode={}",
                appId,
                configuration.integrationType(),
                configuration.sessionMode()
        );
        return new LaunchResponse(
                appId,
                configuration.interfaceConfiguration().entryUrl(),
                session
        );
    }

    private LaunchSessionResponse createSession(
            String appId,
            HubApplicationConfigurationResponse configuration
    ) {
        return switch (configuration.integrationType()) {
            case PARTNER_WEBVIEW -> createPartnerSession(
                    appId,
                    configuration.sessionMode()
            );
            case API_FIRST -> null;
        };
    }

    private LaunchSessionResponse createPartnerSession(String appId, SessionMode sessionMode) {
        return switch (sessionMode) {
            case TOKEN -> new LaunchSessionResponse(
                    SessionMode.TOKEN,
                    partnerApiClient.createSessionToken(appId)
            );
            case CODE -> new LaunchSessionResponse(
                    SessionMode.CODE,
                    partnerApiClient.createSessionCode(appId)
            );
            case NONE -> throw invalidRegistryConfiguration(appId);
        };
    }

    private void validateLaunchConfiguration(
            String appId,
            HubApplicationConfigurationResponse configuration
    ) {
        if (configuration.integrationType() == null
                || configuration.sessionMode() == null
                || configuration.interfaceConfiguration() == null
                || configuration.interfaceConfiguration().entryUrl() == null
                || configuration.interfaceConfiguration().entryUrl().isBlank()) {
            throw invalidRegistryConfiguration(appId);
        }

        boolean compatible = switch (configuration.integrationType()) {
            case PARTNER_WEBVIEW -> configuration.sessionMode() == SessionMode.TOKEN
                    || configuration.sessionMode() == SessionMode.CODE;
            case API_FIRST -> configuration.sessionMode() == SessionMode.NONE;
        };
        if (!compatible) {
            throw invalidRegistryConfiguration(appId);
        }
    }

    private AppRegistryUnavailableException invalidRegistryConfiguration(String appId) {
        log.warn("App-registry returned invalid launch configuration: appId={}", appId);
        return new AppRegistryUnavailableException();
    }
}
