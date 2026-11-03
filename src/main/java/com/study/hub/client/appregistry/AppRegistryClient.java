package com.study.hub.client.appregistry;

import com.study.hub.client.appregistry.dto.ApplicationRoutingResponse;
import com.study.hub.client.appregistry.dto.CatalogGroupResponse;
import com.study.hub.client.appregistry.dto.HubApplicationConfigurationResponse;
import com.study.hub.client.appregistry.dto.PaymentFieldMappingResponse;
import com.study.hub.exception.AppRegistryUnavailableException;
import com.study.hub.exception.ApplicationNotFoundException;
import com.study.hub.exception.CatalogGroupNotFoundException;
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

    public ApplicationRoutingResponse getRoutingByAppId(String appId) {
        ApplicationRoutingResponse routing = execute(
                "get-application-routing",
                appId,
                () -> restClient.get()
                        .uri(
                                "/internal/api/v1/applications/{appId}/routing",
                                appId
                        )
                        .retrieve()
                        .body(ApplicationRoutingResponse.class),
                ApplicationNotFoundException::new
        );

        if (routing == null
                || routing.appId() == null
                || !appId.equals(routing.appId())
                || routing.appId().isBlank()
                || routing.status() == null
                || hasInvalidFieldMappings(routing.paymentFieldMappings())) {
            log.warn(
                    "App-registry returned an empty or incomplete routing response: appId={}",
                    appId
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

        return validateCatalogGroup(catalog, "ecosystem");
    }

    public CatalogGroupResponse getCatalogGroup(String groupId) {
        CatalogGroupResponse group = execute(
                "get-catalog-group",
                groupId,
                () -> restClient.get()
                        .uri(
                                "/internal/api/v1/catalog/groups/{groupId}",
                                groupId
                        )
                        .retrieve()
                        .body(CatalogGroupResponse.class),
                () -> new CatalogGroupNotFoundException(groupId)
        );
        return validateCatalogGroup(group, groupId);
    }

    private CatalogGroupResponse validateCatalogGroup(
            CatalogGroupResponse group,
            String expectedGroupId
    ) {
        if (group == null
                || !expectedGroupId.equals(group.id())
                || group.layout() == null
                || group.title() == null
                || group.items() == null) {
            log.warn(
                    "App-registry returned an empty or incomplete catalog group: groupId={}",
                    expectedGroupId
            );
            throw new AppRegistryUnavailableException();
        }
        return group;
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
