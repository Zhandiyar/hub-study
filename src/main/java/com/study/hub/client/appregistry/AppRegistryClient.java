package com.study.hub.client.appregistry;

import com.study.hub.client.appregistry.dto.HubApplicationConfigurationResponse;
import com.study.hub.client.appregistry.dto.CatalogGroupResponse;
import com.study.hub.client.appregistry.dto.ApplicationRoutingResponse;
import com.study.hub.client.appregistry.dto.PaymentFieldMappingResponse;
import com.study.hub.exception.AppRegistryUnavailableException;
import com.study.hub.exception.ApplicationNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Component
public class AppRegistryClient {

    private final RestClient restClient;

    public AppRegistryClient(@Qualifier("appRegistryRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public HubApplicationConfigurationResponse getApplicationConfiguration(String appId) {
        HubApplicationConfigurationResponse configuration = execute(
                "get-application-configuration",
                appId,
                () -> restClient.get()
                        .uri("/internal/api/v1/applications/{appId}/configuration", appId)
                        .retrieve()
                        .body(HubApplicationConfigurationResponse.class),
                ApplicationNotFoundException::new
        );

        if (configuration == null || configuration.status() == null) {
            log.warn(
                    "App-registry returned an empty or incomplete application response: appId={}",
                    appId
            );
            throw new AppRegistryUnavailableException();
        }
        if (!appId.equals(configuration.appId())) {
            log.warn(
                    "App-registry returned configuration for another application: "
                            + "requestedAppId={} actualAppId={}",
                    appId,
                    configuration.appId()
            );
            throw new AppRegistryUnavailableException();
        }
        return configuration;
    }

    public ApplicationRoutingResponse getRoutingByServiceId(Long serviceId) {
        ApplicationRoutingResponse routing = execute(
                "get-application-routing",
                serviceId,
                () -> restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/internal/api/v1/applications/by-service-id")
                                .queryParam("serviceId", serviceId)
                                .build())
                        .retrieve()
                        .body(ApplicationRoutingResponse.class),
                ApplicationNotFoundException::new
        );

        if (routing == null
                || !serviceId.equals(routing.serviceId())
                || routing.appId() == null
                || routing.appId().isBlank()
                || routing.groupId() == null
                || routing.groupId().isBlank()
                || routing.status() == null
                || routing.contractNumberServiceFieldId() == null
                || routing.contractNumberServiceFieldId() <= 0
                || hasInvalidFieldMappings(routing.paymentFieldMappings())) {
            log.warn(
                    "App-registry returned an empty or incomplete routing response: serviceId={}",
                    serviceId
            );
            throw new AppRegistryUnavailableException();
        }
        return routing;
    }

    private boolean hasInvalidFieldMappings(
            List<PaymentFieldMappingResponse> mappings
    ) {
        return mappings == null || mappings.stream().anyMatch(mapping ->
                mapping == null
                        || mapping.code() == null
                        || mapping.code().isBlank()
                        || mapping.serviceFieldId() == null
                        || mapping.serviceFieldId() <= 0
        );
    }

    public CatalogGroupResponse getCatalog() {
        CatalogGroupResponse catalog = execute(
                "get-catalog",
                "ecosystem",
                () -> restClient.get()
                        .uri("/internal/api/v1/catalog")
                        .retrieve()
                        .body(CatalogGroupResponse.class),
                AppRegistryUnavailableException::new
        );

        if (catalog == null
                || catalog.id() == null
                || catalog.layout() == null
                || catalog.title() == null
                || catalog.items() == null) {
            log.warn("App-registry returned an empty or incomplete catalog response");
            throw new AppRegistryUnavailableException();
        }
        return catalog;
    }

    private <T> T execute(
            String operation,
            Object resourceId,
            Supplier<T> request,
            Supplier<? extends RuntimeException> notFoundException
    ) {
        try {
            return request.get();
        } catch (HttpClientErrorException.NotFound exception) {
            throw notFoundException.get();
        } catch (RestClientResponseException exception) {
            log.warn(
                    "App-registry returned an unexpected status: operation={} resourceId={} status={}",
                    operation,
                    resourceId,
                    exception.getStatusCode().value()
            );
            throw new AppRegistryUnavailableException();
        } catch (RestClientException exception) {
            log.warn(
                    "Could not call app-registry: operation={} resourceId={} errorType={}",
                    operation,
                    resourceId,
                    exception.getClass().getSimpleName()
            );
            throw new AppRegistryUnavailableException(exception);
        }
    }
}
