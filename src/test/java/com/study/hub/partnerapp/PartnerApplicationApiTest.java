package com.study.hub.partnerapp;

import com.study.hub.exception.ApplicationNotFoundException;
import com.study.hub.exception.AppRegistryUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PartnerApplicationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PartnerApplicationService applicationService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void requiresJwt() throws Exception {
        mockMvc.perform(get("/hub/api/v1/insurance-app"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void rejectsInvalidAppIdAsRfc9457Problem() throws Exception {
        mockMvc.perform(get("/hub/api/v1/Insurance-App").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.appId").isArray());
    }

    @Test
    void returnsActiveApplicationWithoutInternalFinancialConfiguration() throws Exception {
        when(applicationService.getApplication("insurance-app"))
                .thenReturn(activeResponse());

        mockMvc.perform(get("/hub/api/v1/insurance-app").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.appId").value("insurance-app"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.sessionMode").value("TOKEN"))
                .andExpect(jsonPath("$.financialConfiguration").doesNotExist())
                .andExpect(jsonPath("$.credentialId").doesNotExist());

        verify(applicationService).getApplication("insurance-app");
    }

    @Test
    void returnsMaintenanceStatusAndMessages() throws Exception {
        when(applicationService.getApplication("insurance-app"))
                .thenReturn(statusResponse(
                        ApplicationStatus.MAINTENANCE,
                        maintenanceMessages()
                ));

        mockMvc.perform(get("/hub/api/v1/insurance-app").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.status").value("MAINTENANCE"))
                .andExpect(jsonPath("$.maintenanceMessages.ru")
                        .value("Технические работы до 18:00"))
                .andExpect(jsonPath("$.maintenanceMessages.kk")
                        .value("Техникалық жұмыстар сағат 18:00-ге дейін"))
                .andExpect(jsonPath("$.maintenanceMessages.en")
                        .value("Maintenance until 18:00"));
    }

    @Test
    void returnsDisabledStatus() throws Exception {
        when(applicationService.getApplication("insurance-app"))
                .thenReturn(statusResponse(ApplicationStatus.DISABLED, null));

        mockMvc.perform(get("/hub/api/v1/insurance-app").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.status").value("DISABLED"));
    }

    @Test
    void returnsMissingApplicationAsNotFound() throws Exception {
        when(applicationService.getApplication("insurance-app"))
                .thenThrow(new ApplicationNotFoundException());

        mockMvc.perform(get("/hub/api/v1/insurance-app").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.code").value("APPLICATION_NOT_FOUND"));
    }

    @Test
    void hidesAppRegistryFailureBehindPublicDependencyError() throws Exception {
        when(applicationService.getApplication("insurance-app"))
                .thenThrow(new AppRegistryUnavailableException());

        mockMvc.perform(get("/hub/api/v1/insurance-app").with(jwt()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DEPENDENCY_UNAVAILABLE"))
                .andExpect(jsonPath("$.detail")
                        .value("Required configuration service is temporarily unavailable"));
    }

    private PartnerApplicationResponse activeResponse() {
        return new PartnerApplicationResponse(
                "insurance-app",
                "insurance",
                ApplicationStatus.ACTIVE,
                null,
                3,
                SessionMode.TOKEN,
                new PartnerApplicationResponse.InterfaceConfiguration(
                        "https://partner.example/app",
                        "Страхование"
                ),
                new PartnerApplicationResponse.NativeSecurityConfiguration(
                        List.of("partner.example"),
                        "default-src 'self'",
                        List.of("close")
                )
        );
    }

    private PartnerApplicationResponse statusResponse(
            ApplicationStatus status,
            PartnerApplicationResponse.MaintenanceMessages maintenanceMessages
    ) {
        return new PartnerApplicationResponse(
                "insurance-app",
                "insurance",
                status,
                maintenanceMessages,
                3,
                null,
                null,
                null
        );
    }

    private PartnerApplicationResponse.MaintenanceMessages maintenanceMessages() {
        return new PartnerApplicationResponse.MaintenanceMessages(
                "Технические работы до 18:00",
                "Техникалық жұмыстар сағат 18:00-ге дейін",
                "Maintenance until 18:00"
        );
    }
}
