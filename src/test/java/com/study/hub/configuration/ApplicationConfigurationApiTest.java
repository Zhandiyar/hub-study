package com.study.hub.configuration;

import com.study.hub.client.appregistry.AppRegistryClient;
import com.study.hub.client.appregistry.dto.HubApplicationConfigurationResponse;
import com.study.hub.client.appregistry.dto.FinancialConfigurationResponse;
import com.study.hub.client.appregistry.dto.InterfaceConfigurationResponse;
import com.study.hub.client.appregistry.dto.LocalizedTextResponse;
import com.study.hub.client.appregistry.dto.MaintenanceMessagesResponse;
import com.study.hub.client.appregistry.dto.NativeSecurityConfigurationResponse;
import com.study.hub.exception.AppRegistryUnavailableException;
import com.study.hub.exception.ApplicationNotFoundException;
import com.study.hub.client.appregistry.model.ApplicationStatus;
import com.study.hub.client.appregistry.model.IntegrationType;
import com.study.hub.client.appregistry.model.SessionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationConfigurationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppRegistryClient appRegistryClient;

    @Test
    void returnsApplicationConfigurationWithoutChangingAppRegistryContract() throws Exception {
        when(appRegistryClient.getApplicationConfiguration("insurance-app"))
                .thenReturn(activeResponse());

        mockMvc.perform(get("/hub/api/v1/applications/insurance-app"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.appId").value("insurance-app"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.integrationType").value("PARTNER_WEBVIEW"))
                .andExpect(jsonPath("$.sessionMode").value("TOKEN"))
                .andExpect(jsonPath("$.financialConfiguration.merchantId")
                        .value("merchant-123"))
                .andExpect(jsonPath("$.financialConfiguration.serviceId")
                        .value(456))
                .andExpect(jsonPath("$.financialConfiguration.paymentWebhookUrl")
                        .doesNotExist())
                .andExpect(jsonPath("$.financialConfiguration.credentialId")
                        .doesNotExist());

        verify(appRegistryClient).getApplicationConfiguration("insurance-app");
    }

    @Test
    void returnsMaintenanceStatusAndMessages() throws Exception {
        when(appRegistryClient.getApplicationConfiguration("insurance-app"))
                .thenReturn(statusResponse(
                        ApplicationStatus.MAINTENANCE,
                        new MaintenanceMessagesResponse(
                                "Технические работы до 18:00",
                                "Техникалық жұмыстар 18:00-ге дейін",
                                "Maintenance until 18:00"
                        )
                ));

        mockMvc.perform(get("/hub/api/v1/applications/insurance-app"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MAINTENANCE"))
                .andExpect(jsonPath("$.maintenanceMessages.ru")
                        .value("Технические работы до 18:00"));
    }

    @Test
    void returnsDisabledStatus() throws Exception {
        when(appRegistryClient.getApplicationConfiguration("insurance-app"))
                .thenReturn(statusResponse(ApplicationStatus.DISABLED, null));

        mockMvc.perform(get("/hub/api/v1/applications/insurance-app"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"))
                .andExpect(jsonPath("$.maintenanceMessages").doesNotExist())
                .andExpect(jsonPath("$.financialConfiguration").hasJsonPath())
                .andExpect(jsonPath("$.financialConfiguration").value(nullValue()));
    }

    @Test
    void returnsMissingApplicationAsNotFound() throws Exception {
        when(appRegistryClient.getApplicationConfiguration("insurance-app"))
                .thenThrow(new ApplicationNotFoundException());

        mockMvc.perform(get("/hub/api/v1/applications/insurance-app"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.code").value("APPLICATION_NOT_FOUND"));
    }

    @Test
    void hidesAppRegistryFailureBehindPublicDependencyError() throws Exception {
        when(appRegistryClient.getApplicationConfiguration("insurance-app"))
                .thenThrow(new AppRegistryUnavailableException());

        mockMvc.perform(get("/hub/api/v1/applications/insurance-app"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DEPENDENCY_UNAVAILABLE"))
                .andExpect(jsonPath("$.detail")
                        .value("Required configuration service is temporarily unavailable"));
    }

    @Test
    void doesNotExposeLegacyApplicationPath() throws Exception {
        mockMvc.perform(get("/hub/api/v1/insurance-app"))
                .andExpect(status().isNotFound());
    }

    private HubApplicationConfigurationResponse activeResponse() {
        return new HubApplicationConfigurationResponse(
                "insurance-app",
                "insurance",
                ApplicationStatus.ACTIVE,
                null,
                3,
                IntegrationType.PARTNER_WEBVIEW,
                SessionMode.TOKEN,
                new InterfaceConfigurationResponse(
                        "https://partner.example/app",
                        new LocalizedTextResponse(
                                "Страхование",
                                "Сақтандыру",
                                "Insurance"
                        ),
                        null,
                        10
                ),
                new NativeSecurityConfigurationResponse(
                        List.of("partner.example"),
                        "default-src 'self'",
                        List.of("close")
                ),
                new FinancialConfigurationResponse(
                        "merchant-123",
                        456L,
                        901L,
                        List.of(),
                        "https://partner.example/webhooks/payment",
                        "insurance-app-credential"
                )
        );
    }

    private HubApplicationConfigurationResponse statusResponse(
            ApplicationStatus status,
            MaintenanceMessagesResponse maintenanceMessages
    ) {
        return new HubApplicationConfigurationResponse(
                "insurance-app",
                "insurance",
                status,
                maintenanceMessages,
                3,
                null,
                null,
                null,
                null,
                null
        );
    }
}
