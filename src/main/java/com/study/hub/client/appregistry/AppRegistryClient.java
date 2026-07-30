package com.study.hub.client.appregistry;

import com.study.hub.exception.ApplicationNotFoundException;
import com.study.hub.exception.AppRegistryUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import static org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver.clientRegistrationId;
import static org.springframework.security.oauth2.client.web.client.RequestAttributePrincipalResolver.principal;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppRegistryClient {

    private static final String CLIENT_REGISTRATION_ID = "app-registry";
    private static final String APPLICATION_PRINCIPAL = "hub";

    @Qualifier("appRegistryRestClient")
    private final RestClient restClient;

    public AppRegistryApplicationConfiguration getApplicationConfiguration(String appId) {
        try {
            AppRegistryApplicationConfiguration configuration = restClient.get()
                    .uri("/internal/api/v1/applications/{appId}/configuration", appId)
                    .attributes(clientRegistrationId(CLIENT_REGISTRATION_ID))
                    .attributes(principal(APPLICATION_PRINCIPAL))
                    .retrieve()
                    .body(AppRegistryApplicationConfiguration.class);

            if (configuration == null
                    || configuration.status() == null
                    || !appId.equals(configuration.appId())) {
                throw new AppRegistryUnavailableException();
            }
            return configuration;
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ApplicationNotFoundException();
        } catch (RestClientResponseException exception) {
            log.warn(
                    "App-registry returned an unexpected status: status={}",
                    exception.getStatusCode().value()
            );
            throw new AppRegistryUnavailableException(exception);
        } catch (RestClientException exception) {
            log.warn("Could not call app-registry: {}", exception.getMessage());
            throw new AppRegistryUnavailableException(exception);
        } catch (OAuth2AuthorizationException exception) {
            log.warn(
                    "Could not obtain app-registry service token: error={}",
                    exception.getError().getErrorCode()
            );
            throw new AppRegistryUnavailableException(exception);
        }
    }
}
