package com.study.hub.client.appregistry;

import com.study.hub.client.appregistry.dto.HubApplicationConfigurationResponse;
import com.study.hub.client.appregistry.dto.CatalogApplicationResponse;
import com.study.hub.client.appregistry.dto.CatalogGroupResponse;
import com.study.hub.exception.AppRegistryUnavailableException;
import com.study.hub.exception.ApplicationNotFoundException;
import com.study.hub.client.appregistry.model.ApplicationStatus;
import com.study.hub.client.appregistry.model.IntegrationType;
import com.study.hub.client.appregistry.model.SessionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

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
                          "integrationType": "PARTNER_WEBVIEW",
                          "sessionMode": "TOKEN",
                          "interfaceConfiguration": {
                            "entryUrl": "https://partner.example/app",
                            "title": {
                              "ru": "Страхование",
                              "kk": "Сақтандыру",
                              "en": "Insurance"
                            },
                            "iconUrl": null,
                            "displayOrder": 10
                          },
                          "nativeSecurityConfiguration": {
                            "allowedDomains": ["partner.example"],
                            "contentSecurityPolicy": "default-src 'self'",
                            "allowedBridgeMethods": ["close"]
                          },
                          "financialConfiguration": {
                            "merchantId": "merchant-1",
                            "serviceId": "service-1",
                            "paymentWebhookUrl": "https://partner.example/webhook",
                            "credentialId": "partner-app-credential"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        HubApplicationConfigurationResponse result =
                client.getApplicationConfiguration("insurance-app");

        assertThat(result.appId()).isEqualTo("insurance-app");
        assertThat(result.status()).isEqualTo(ApplicationStatus.ACTIVE);
        assertThat(result.integrationType()).isEqualTo(IntegrationType.PARTNER_WEBVIEW);
        assertThat(result.sessionMode()).isEqualTo(SessionMode.TOKEN);
        assertThat(result.financialConfiguration().merchantId()).isEqualTo("merchant-1");
        assertThat(result.financialConfiguration().serviceId()).isEqualTo("service-1");
        assertThat(result.financialConfiguration().credentialId())
                .isEqualTo("partner-app-credential");
        server.verify();
    }

    @Test
    void readsMaintenanceConfigurationFromAppRegistryContract() throws IOException {
        server.expect(requestTo(
                        "http://app-registry/internal/api/v1/applications/insurance-app/configuration"
                ))
                .andRespond(withSuccess(
                        contract("application-configuration-maintenance.json"),
                        MediaType.APPLICATION_JSON
                ));

        HubApplicationConfigurationResponse result =
                client.getApplicationConfiguration("insurance-app");

        assertThat(result.maintenanceMessages().ru()).isEqualTo("Технические работы");
        assertThat(result.maintenanceMessages().kk()).isEqualTo("Техникалық жұмыстар");
        assertThat(result.financialConfiguration().serviceId()).isEqualTo("service-456");
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
                .hasMessage("App-registry is temporarily unavailable")
                .hasNoCause();
        server.verify();
    }

    @Test
    void returnsConfigurationWithoutDuplicatingAppRegistryBusinessValidation() {
        server.expect(requestTo(
                        "http://app-registry/internal/api/v1/applications/insurance-app/configuration"
                ))
                .andRespond(withSuccess("""
                        {
                          "appId": "insurance-app",
                          "groupId": "insurance",
                          "status": "ACTIVE",
                          "maintenanceMessages": null,
                          "version": 3,
                          "sessionMode": null
                        }
                        """, MediaType.APPLICATION_JSON));

        HubApplicationConfigurationResponse result =
                client.getApplicationConfiguration("insurance-app");

        assertThat(result.status()).isEqualTo(ApplicationStatus.ACTIVE);
        assertThat(result.sessionMode()).isNull();
        assertThat(result.interfaceConfiguration()).isNull();
        assertThat(result.nativeSecurityConfiguration()).isNull();
        assertThat(result.financialConfiguration()).isNull();
        server.verify();
    }

    @Test
    void readsApiFirstConfigurationWithoutPartnerSession() {
        server.expect(requestTo(
                        "http://app-registry/internal/api/v1/applications/platform-app/configuration"
                ))
                .andRespond(withSuccess("""
                        {
                          "appId": "platform-app",
                          "groupId": "insurance",
                          "status": "ACTIVE",
                          "maintenanceMessages": null,
                          "version": 1,
                          "integrationType": "API_FIRST",
                          "sessionMode": "NONE",
                          "interfaceConfiguration": {
                            "entryUrl": "https://platform.example/app",
                            "title": {
                              "ru": "Платформа",
                              "kk": "Платформа",
                              "en": "Platform application"
                            },
                            "iconUrl": null,
                            "displayOrder": 10
                          },
                          "nativeSecurityConfiguration": {
                            "allowedDomains": [],
                            "contentSecurityPolicy": "default-src 'self'",
                            "allowedBridgeMethods": []
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        HubApplicationConfigurationResponse result =
                client.getApplicationConfiguration("platform-app");

        assertThat(result.integrationType()).isEqualTo(IntegrationType.API_FIRST);
        assertThat(result.sessionMode()).isEqualTo(SessionMode.NONE);
        server.verify();
    }

    @Test
    void rejectsSuccessfulResponseWithoutConfigurationBody() {
        server.expect(requestTo(
                        "http://app-registry/internal/api/v1/applications/insurance-app/configuration"
                ))
                .andRespond(withSuccess());

        assertThatThrownBy(() -> client.getApplicationConfiguration("insurance-app"))
                .isInstanceOf(AppRegistryUnavailableException.class);
        server.verify();
    }

    @Test
    void rejectsConfigurationForAnotherApplication() {
        server.expect(requestTo(
                        "http://app-registry/internal/api/v1/applications/insurance-app/configuration"
                ))
                .andRespond(withSuccess("""
                        {
                          "appId": "another-app",
                          "groupId": "insurance",
                          "status": "ACTIVE",
                          "version": 1
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getApplicationConfiguration("insurance-app"))
                .isInstanceOf(AppRegistryUnavailableException.class);
        server.verify();
    }

    @Test
    void returnsCatalogTree() {
        server.expect(requestTo("http://app-registry/internal/api/v1/catalog"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id": "ecosystem",
                          "type": "GROUP",
                          "layout": "GRID",
                          "title": {
                            "ru": "Экосистема",
                            "kk": "Экожүйе",
                            "en": "Ecosystem"
                          },
                          "iconUrl": null,
                          "items": [
                            {
                              "id": "insurance",
                              "type": "GROUP",
                              "layout": "LIST",
                              "title": {
                                "ru": "Страхование",
                                "kk": "Сақтандыру",
                                "en": "Insurance"
                              },
                              "iconUrl": null,
                              "items": [
                                {
                                  "id": "insurance-app",
                                  "type": "APPLICATION",
                                  "title": {
                                    "ru": "КАСКО",
                                    "kk": "КАСКО",
                                    "en": "CASCO"
                                  },
                                  "iconUrl": null,
                                  "status": "MAINTENANCE",
                                  "maintenanceMessages": {
                                    "ru": "Технические работы",
                                    "kk": "Техникалық жұмыстар",
                                    "en": "Maintenance"
                                  }
                                }
                              ]
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        CatalogGroupResponse result = client.getCatalog();

        assertThat(result.id()).isEqualTo("ecosystem");
        assertThat(result.items()).singleElement()
                .isInstanceOfSatisfying(CatalogGroupResponse.class, group ->
                        assertThat(group.items()).singleElement()
                                .isInstanceOfSatisfying(
                                        CatalogApplicationResponse.class,
                                        application -> {
                                            assertThat(application.id())
                                                    .isEqualTo("insurance-app");
                                            assertThat(application.status())
                                                    .isEqualTo(ApplicationStatus.MAINTENANCE);
                                            assertThat(application.maintenanceMessages().ru())
                                                    .isEqualTo("Технические работы");
                                        }
                                ));
        server.verify();
    }

    @Test
    void rejectsEmptyCatalogResponse() {
        server.expect(requestTo("http://app-registry/internal/api/v1/catalog"))
                .andRespond(withSuccess());

        assertThatThrownBy(client::getCatalog)
                .isInstanceOf(AppRegistryUnavailableException.class);
        server.verify();
    }

    private String contract(String name) throws IOException {
        try (var input = getClass().getResourceAsStream("/app-registry/" + name)) {
            if (input == null) {
                throw new IOException("Contract fixture not found: " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
