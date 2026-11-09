package com.study.hub.client.partner;

import com.study.hub.exception.OrderNotFoundException;
import com.study.hub.exception.PartnerApiUnavailableException;
import com.study.hub.exception.PartnerBusinessException;
import java.net.URI;
import java.net.URISyntaxException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class PartnerApiClient {
    private static final String ORDER_URL =
            "http://{appId}/internal/api/v1/contracts/{orderId}";
    private static final String SESSION_TOKEN_URL =
            "http://{appId}/internal/api/v1/session/token";
    private static final String SESSION_CODE_URL =
            "http://{appId}/internal/api/v1/session/code";
    private static final String SESSION_VIEW_URL =
            "http://{appId}/internal/api/v1/session/view-url";

    private final RestClient restClient;
    private final PartnerErrorParser errorParser;

    public PartnerApiClient(
            @Qualifier("partnerApiRestClient") RestClient restClient,
            PartnerErrorParser errorParser
    ) {
        this.restClient = restClient;
        this.errorParser = errorParser;
    }

    public PartnerOrderResponse getOrder(
            String appId,
            String orderId,
            String language
    ) {
        try {
            PartnerOrderResponse order = restClient.get()
                    .uri(ORDER_URL, appId, orderId)
                    .header(HttpHeaders.ACCEPT_LANGUAGE, language)
                    .retrieve()
                    .body(PartnerOrderResponse.class);

            if (isInvalidOrder(order, orderId)) {
                log.warn("Partner service returned an invalid order response: appId={}", appId);
                throw new PartnerApiUnavailableException();
            }
            return order;
        } catch (HttpClientErrorException.NotFound exception) {
            throw new OrderNotFoundException();
        } catch (RestClientResponseException exception) {
            throw mapResponseException(appId, "get-order", exception);
        } catch (RestClientException exception) {
            log.warn(
                    "Could not call partner service: appId={} errorType={}",
                    appId,
                    exception.getClass().getSimpleName()
            );
            throw new PartnerApiUnavailableException(exception);
        }
    }

    private boolean isInvalidOrder(
            PartnerOrderResponse order,
            String requestedOrderId
    ) {
        return order == null
                || order.orderId() == null
                || !order.orderId().equals(requestedOrderId)
                || order.status() == null
                || order.status().isBlank()
                || order.amount() == null;
    }

    public String createSessionToken(String appId, String language) {
        PartnerSessionTokenResponse response = createSession(
                appId,
                language,
                SESSION_TOKEN_URL,
                PartnerSessionTokenResponse.class
        );
        if (response.token() == null || response.token().isBlank()) {
            throw invalidSessionResponse(appId);
        }
        return response.token();
    }

    public String createSessionCode(String appId, String language) {
        PartnerSessionCodeResponse response = createSession(
                appId,
                language,
                SESSION_CODE_URL,
                PartnerSessionCodeResponse.class
        );
        if (response.code() == null || response.code().isBlank()) {
            throw invalidSessionResponse(appId);
        }
        return response.code();
    }

    public String createViewUrl(String appId, String language) {
        PartnerViewUrlResponse response = createSession(
                appId,
                language,
                SESSION_VIEW_URL,
                PartnerViewUrlResponse.class
        );
        if (response.entryUrl() == null || !isSafeViewUrl(response.entryUrl())) {
            throw invalidSessionResponse(appId);
        }
        return response.entryUrl();
    }

    private <T> T createSession(
            String appId,
            String language,
            String url,
            Class<T> responseType
    ) {
        try {
            T response = restClient.post()
                    .uri(url, appId)
                    .header(HttpHeaders.ACCEPT_LANGUAGE, language)
                    .retrieve()
                    .body(responseType);

            if (response == null) {
                throw invalidSessionResponse(appId);
            }
            return response;
        } catch (RestClientResponseException exception) {
            throw mapResponseException(appId, "create-session", exception);
        } catch (RestClientException exception) {
            log.warn(
                    "Could not create partner session: appId={} errorType={}",
                    appId,
                    exception.getClass().getSimpleName()
            );
            throw new PartnerApiUnavailableException(exception);
        }
    }

    private PartnerApiUnavailableException invalidSessionResponse(String appId) {
        log.warn("Partner service returned an invalid session response: appId={}", appId);
        return new PartnerApiUnavailableException();
    }

    private boolean isSafeViewUrl(String value) {
        try {
            URI uri = new URI(value);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && uri.getRawUserInfo() == null
                    && uri.getRawFragment() == null;
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    private RuntimeException mapResponseException(
            String appId,
            String operation,
            RestClientResponseException exception
    ) {
        int status = exception.getStatusCode().value();
        if (status == 422) {
            PartnerErrorResponse error = errorParser.parse(exception.getResponseBodyAsString());
            log.warn(
                    "Partner service rejected request: appId={} operation={} status={} errorCode={}",
                    appId,
                    operation,
                    status,
                    error.errorCode()
            );
            return new PartnerBusinessException(
                    error.detailOr("Partner service rejected the request"),
                    error.errorCode(),
                    error.errors()
            );
        }

        log.warn(
                "Partner service returned an unexpected status: appId={} operation={} status={}",
                appId,
                operation,
                status
        );
        return new PartnerApiUnavailableException();
    }

    private record PartnerSessionTokenResponse(String token) {
    }

    private record PartnerSessionCodeResponse(String code) {
    }

    private record PartnerViewUrlResponse(String entryUrl) {
    }
}
