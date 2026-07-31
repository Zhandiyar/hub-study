package com.study.hub.partnerapp;

import com.study.hub.exception.ApplicationMaintenanceException;
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
import java.util.Locale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    private static final Locale RUSSIAN = Locale.forLanguageTag("ru");
    private static final Locale KAZAKH = Locale.forLanguageTag("kk");

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
    void returnsActiveApplicationWithoutInternalFinancialConfiguration() throws Exception {
        when(applicationService.getApplication(eq("insurance-app"), any(Locale.class)))
                .thenReturn(response());

        mockMvc.perform(get("/hub/api/v1/insurance-app").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.appId").value("insurance-app"))
                .andExpect(jsonPath("$.sessionMode").value("TOKEN"))
                .andExpect(jsonPath("$.financialConfiguration").doesNotExist())
                .andExpect(jsonPath("$.credentialId").doesNotExist());

        verify(applicationService).getApplication("insurance-app", RUSSIAN);
    }

    @Test
    void passesRequestedLanguageToService() throws Exception {
        when(applicationService.getApplication("insurance-app", KAZAKH))
                .thenReturn(response());

        mockMvc.perform(get("/hub/api/v1/insurance-app")
                        .header("Accept-Language", "kk")
                        .with(jwt()))
                .andExpect(status().isOk());

        verify(applicationService).getApplication("insurance-app", KAZAKH);
    }

    @Test
    void returnsMaintenanceAsRfc9457Problem() throws Exception {
        when(applicationService.getApplication(eq("insurance-app"), any(Locale.class)))
                .thenThrow(new ApplicationMaintenanceException(
                        "Технические работы до 18:00"
                ));

        mockMvc.perform(get("/hub/api/v1/insurance-app").with(jwt()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.code").value("APPLICATION_MAINTENANCE"))
                .andExpect(jsonPath("$.detail").value("Технические работы до 18:00"));
    }

    @Test
    void returnsDisabledOrMissingApplicationAsNotFound() throws Exception {
        when(applicationService.getApplication(eq("insurance-app"), any(Locale.class)))
                .thenThrow(new ApplicationNotFoundException());

        mockMvc.perform(get("/hub/api/v1/insurance-app").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.code").value("APPLICATION_NOT_FOUND"));
    }

    @Test
    void hidesAppRegistryFailureBehindPublicDependencyError() throws Exception {
        when(applicationService.getApplication(eq("insurance-app"), any(Locale.class)))
                .thenThrow(new AppRegistryUnavailableException());

        mockMvc.perform(get("/hub/api/v1/insurance-app").with(jwt()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DEPENDENCY_UNAVAILABLE"))
                .andExpect(jsonPath("$.detail")
                        .value("Required configuration service is temporarily unavailable"));
    }

    private PartnerApplicationResponse response() {
        return new PartnerApplicationResponse(
                "insurance-app",
                "insurance",
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
}
