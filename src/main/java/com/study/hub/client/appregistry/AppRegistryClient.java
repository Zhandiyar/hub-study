package com.study.hub.client.appregistry;

import com.study.hub.exception.AppRegistryUnavailableException;
import com.study.hub.exception.ApplicationGroupNotFoundException;
import com.study.hub.exception.ApplicationNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

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
        AppRegistryApplicationConfiguration configuration = execute(
                () -> restClient.get()
                        .uri("/internal/api/v1/applications/{appId}/configuration", appId)
                        .attributes(clientRegistrationId(CLIENT_REGISTRATION_ID))
                        .attributes(principal(APPLICATION_PRINCIPAL))
                        .retrieve()
                        .body(AppRegistryApplicationConfiguration.class),
                ApplicationNotFoundException::new
        );

        if (configuration == null || configuration.status() == null) {
            log.warn("App-registry returned an empty or incomplete application response");
            throw new AppRegistryUnavailableException();
        }
        return configuration;
    }

    public AppRegistryApplicationGroup getApplicationGroup(
            String groupId,
            int page,
            int size
    ) {
        AppRegistryApplicationGroup group = execute(
                () -> restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/internal/api/v1/groups/{groupId}/applications")
                                .queryParam("page", page)
                                .queryParam("size", size)
                                .build(groupId))
                        .attributes(clientRegistrationId(CLIENT_REGISTRATION_ID))
                        .attributes(principal(APPLICATION_PRINCIPAL))
                        .retrieve()
                        .body(AppRegistryApplicationGroup.class),
                ApplicationGroupNotFoundException::new
        );

        if (group == null || group.applications() == null) {
            log.warn("App-registry returned an empty or incomplete group response");
            throw new AppRegistryUnavailableException();
        }
        return group;
    }

    private <T> T execute(
            Supplier<T> request,
            Supplier<? extends RuntimeException> notFoundException
    ) {
        try {
            return request.get();
        } catch (HttpClientErrorException.NotFound exception) {
            throw notFoundException.get();
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
