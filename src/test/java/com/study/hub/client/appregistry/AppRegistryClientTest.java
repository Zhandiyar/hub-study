package com.study.hub.client.appregistry;

import com.study.hub.client.appregistry.dto.HubApplicationConfigurationResponse;
import com.study.hub.client.appregistry.dto.ApplicationRoutingResponse;
import com.study.hub.client.appregistry.dto.CatalogApplicationResponse;
import com.study.hub.client.appregistry.dto.CatalogGroupResponse;
import com.study.hub.client.appregistry.dto.CatalogListItemResponse;
import com.study.hub.client.appregistry.dto.PaymentFieldMappingResponse;
import com.study.hub.exception.AppRegistryUnavailableException;
import com.study.hub.exception.ApplicationNotFoundException;
import com.study.hub.exception.CatalogGroupNotFoundException;
import com.study.hub.client.appregistry.model.ApplicationStatus;
import com.study.hub.client.appregistry.model.IntegrationType;
import com.study.hub.client.appregistry.model.SessionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
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
                            "serviceId": 1,
                            "contractNumberServiceFieldId": 901,
                            "notificationFields": [],
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
        assertThat(result.financialConfiguration().serviceId()).isEqualTo(1L);
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
        assertThat(result.financialConfiguration().serviceId()).isEqualTo(456L);
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
    void returnsApplicationRoutingByServiceId() {
        server.expect(once(), requestTo(
                        "http://app-registry/internal/api/v1/applications/by-service-id"
                                + "?serviceId=456"
                ))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "serviceId": 456,
                          "groupId": "insurance",
                          "appId": "insurance-app",
                          "status": "ACTIVE",
                          "contractNumberServiceFieldId": 901,
                          "paymentFieldMappings": [
                            {"code": "type", "serviceFieldId": 902}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        ApplicationRoutingResponse result =
                client.getRoutingByServiceId(456L);

        assertThat(result.serviceId()).isEqualTo(456L);
        assertThat(result.groupId()).isEqualTo("insurance");
        assertThat(result.appId()).isEqualTo("insurance-app");
        assertThat(result.status()).isEqualTo(ApplicationStatus.ACTIVE);
        assertThat(result.contractNumberServiceFieldId()).isEqualTo(901L);
        assertThat(result.paymentFieldMappings()).containsExactly(
                new PaymentFieldMappingResponse("type", 902L)
        );
        server.verify();
    }

    @Test
    void mapsUnknownServiceIdToApplicationNotFound() {
        server.expect(requestTo(
                        "http://app-registry/internal/api/v1/applications/by-service-id"
                                + "?serviceId=999"
                ))
                .andRespond(withResourceNotFound());

        assertThatThrownBy(() -> client.getRoutingByServiceId(999L))
                .isInstanceOf(ApplicationNotFoundException.class);
        server.verify();
    }

    @Test
    void rejectsIncompleteApplicationRouting() {
        server.expect(requestTo(
                        "http://app-registry/internal/api/v1/applications/by-service-id"
                                + "?serviceId=456"
                ))
                .andRespond(withSuccess("""
                        {
                          "serviceId": 456,
                          "groupId": "insurance",
                          "appId": "insurance-app",
                          "contractNumberServiceFieldId": 901,
                          "paymentFieldMappings": []
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getRoutingByServiceId(456L))
                .isInstanceOf(AppRegistryUnavailableException.class);
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
    void mapsUnauthorizedResponseToDependencyUnavailable() {
        server.expect(requestTo(
                        "http://app-registry/internal/api/v1/applications/by-service-id"
                                + "?serviceId=456"
                ))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.getRoutingByServiceId(456L))
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

    @Test
    void returnsFlatCatalogGroupItems() {
        server.expect(requestTo(
                        "http://app-registry/internal/api/v1/catalog/groups/ecosystem/items"
                ))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {
                            "id": "travel-app",
                            "type": "APPLICATION",
                            "title": {
                              "ru": "Путешествия",
                              "kk": "Саяхат",
                              "en": "Travel"
                            },
                            "iconUrl": null
                          },
                          {
                            "id": "insurance",
                            "type": "GROUP",
                            "title": {
                              "ru": "Страхование",
                              "kk": "Сақтандыру",
                              "en": "Insurance"
                            },
                            "iconUrl": null
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<CatalogListItemResponse> result =
                client.getCatalogGroupItems("ecosystem");

        assertThat(result).extracting(CatalogListItemResponse::id)
                .containsExactly("travel-app", "insurance");
        assertThat(result.getFirst().type().name()).isEqualTo("APPLICATION");
        server.verify();
    }

    @Test
    void mapsUnknownCatalogGroupToNotFound() {
        server.expect(requestTo(
                        "http://app-registry/internal/api/v1/catalog/groups/missing/items"
                ))
                .andRespond(withResourceNotFound());

        assertThatThrownBy(() -> client.getCatalogGroupItems("missing"))
                .isInstanceOf(CatalogGroupNotFoundException.class);
        server.verify();
    }

    @Test
    void rejectsIncompleteFlatCatalogItem() {
        server.expect(requestTo(
                        "http://app-registry/internal/api/v1/catalog/groups/ecosystem/items"
                ))
                .andRespond(withSuccess("""
                        [{"id":"travel-app","type":"APPLICATION"}]
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getCatalogGroupItems("ecosystem"))
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
