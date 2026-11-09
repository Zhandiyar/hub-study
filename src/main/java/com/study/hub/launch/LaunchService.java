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

    public LaunchResponse launch(String appId, String language) {
        HubApplicationConfigurationResponse configuration =
                applicationAvailabilityService.getActiveConfiguration(appId);

        validateLaunchConfiguration(appId, configuration);
        PreparedLaunch prepared = prepareLaunch(appId, language, configuration);

        log.info(
                "Application launch prepared: appId={} integrationType={} sessionMode={}",
                appId,
                configuration.integrationType(),
                configuration.sessionMode()
        );
        return new LaunchResponse(
                appId,
                prepared.entryUrl(),
                prepared.session()
        );
    }

    private PreparedLaunch prepareLaunch(
            String appId,
            String language,
            HubApplicationConfigurationResponse configuration
    ) {
        return switch (configuration.integrationType()) {
            case PARTNER_WEBVIEW -> preparePartnerWebview(
                    appId,
                    language,
                    configuration
            );
            case API_FIRST -> new PreparedLaunch(
                    configuration.interfaceConfiguration().entryUrl(),
                    null
            );
        };
    }

    private PreparedLaunch preparePartnerWebview(
            String appId,
            String language,
            HubApplicationConfigurationResponse configuration
    ) {
        String configuredEntryUrl = configuration.interfaceConfiguration().entryUrl();
        return switch (configuration.sessionMode()) {
            case TOKEN -> new PreparedLaunch(configuredEntryUrl, new LaunchSessionResponse(
                    SessionMode.TOKEN,
                    partnerApiClient.createSessionToken(appId, language)
            ));
            case CODE -> new PreparedLaunch(configuredEntryUrl, new LaunchSessionResponse(
                    SessionMode.CODE,
                    partnerApiClient.createSessionCode(appId, language)
            ));
            case VIEW_URL -> new PreparedLaunch(
                    partnerApiClient.createViewUrl(appId, language),
                    new LaunchSessionResponse(SessionMode.VIEW_URL, null)
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
                    || configuration.sessionMode() == SessionMode.CODE
                    || configuration.sessionMode() == SessionMode.VIEW_URL;
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

    private record PreparedLaunch(String entryUrl, LaunchSessionResponse session) {
    }
}
