package com.study.hub.client.appregistry;

import com.study.hub.exception.ApplicationNotFoundException;
import com.study.hub.exception.AppRegistryUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AppRegistryClientTest {

    private MockRestServiceServer server;
    private AppRegistryClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://app-registry");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AppRegistryClient(builder.build());
    }

    @Test
    void returnsApplicationConfiguration() {
        server.expect(once(), requestTo(
                        "http://app-registry/internal/api/v1/applications/insurance-app/configuration"
                ))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "appId": "insurance-app",
                          "groupId": "insurance",
                          "status": "ACTIVE",
                          "maintenanceMessages": null,
                          "version": 3,
                          "sessionMode": "TOKEN",
                          "interfaceConfiguration": {
                            "entryUrl": "https://partner.example/app",
                            "title": "Страхование"
                          },
                          "nativeSecurityConfiguration": {
                            "allowedDomains": ["partner.example"],
                            "contentSecurityPolicy": "default-src 'self'",
                            "allowedBridgeMethods": ["close"]
                          },
                          "financialConfiguration": {
                            "merchantId": "merchant-1",
                            "paymentWebhookUrl": "https://partner.example/webhook",
                            "credentialId": "partner-app-credential"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        AppRegistryApplicationConfiguration result =
                client.getApplicationConfiguration("insurance-app");

        assertThat(result.appId()).isEqualTo("insurance-app");
        assertThat(result.status())
                .isEqualTo(AppRegistryApplicationConfiguration.ApplicationStatus.ACTIVE);
        assertThat(result.sessionMode()).isEqualTo(com.study.hub.partnerapp.SessionMode.TOKEN);
        assertThat(result.financialConfiguration().credentialId())
                .isEqualTo("partner-app-credential");
        server.verify();
    }

    @Test
    void readsLocalizedMaintenanceMessages() {
        server.expect(requestTo(
                        "http://app-registry/internal/api/v1/applications/insurance-app/configuration"
                ))
                .andRespond(withSuccess("""
                        {
                          "appId": "insurance-app",
                          "groupId": "insurance",
                          "status": "MAINTENANCE",
                          "maintenanceMessages": {
                            "ru": "Технические работы",
                            "kk": "Техникалық жұмыстар",
                            "en": "Maintenance"
                          },
                          "version": 4,
                          "sessionMode": "TOKEN"
                        }
                        """, MediaType.APPLICATION_JSON));

        AppRegistryApplicationConfiguration result =
                client.getApplicationConfiguration("insurance-app");

        assertThat(result.maintenanceMessages().ru()).isEqualTo("Технические работы");
        assertThat(result.maintenanceMessages().kk()).isEqualTo("Техникалық жұмыстар");
        assertThat(result.maintenanceMessages().en()).isEqualTo("Maintenance");
        server.verify();
    }

    @Test
    void mapsNotFoundResponse() {
        server.expect(requestTo(
                        "http://app-registry/internal/api/v1/applications/missing/configuration"
                ))
                .andRespond(withResourceNotFound());

        assertThatThrownBy(() -> client.getApplicationConfiguration("missing"))
                .isInstanceOf(ApplicationNotFoundException.class);
        server.verify();
    }

    @Test
    void mapsServerErrorWithoutExposingResponseBody() {
        server.expect(requestTo(
                        "http://app-registry/internal/api/v1/applications/insurance-app/configuration"
                ))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.getApplicationConfiguration("insurance-app"))
                .isInstanceOf(AppRegistryUnavailableException.class)
                .hasMessage("App-registry is temporarily unavailable");
        server.verify();
    }
}
