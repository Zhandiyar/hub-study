package com.study.hub.launch;

import com.study.hub.client.appregistry.model.SessionMode;
import com.study.hub.exception.ApplicationUnavailableException;
import com.study.hub.exception.PartnerApiUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LaunchApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LaunchService launchService;

    @Test
    void returnsLaunchDataWithoutCaching() throws Exception {
        when(launchService.launch("insurance-app", "kk-KZ")).thenReturn(new LaunchResponse(
                "insurance-app",
                "https://partner.example.com/app",
                new LaunchSessionResponse(SessionMode.TOKEN, "partner-token")
        ));

        mockMvc.perform(post("/hub/api/v1/applications/insurance-app/launch")
                        .header("Accept-Language", "kk-KZ"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.appId").value("insurance-app"))
                .andExpect(jsonPath("$.entryUrl").value("https://partner.example.com/app"))
                .andExpect(jsonPath("$.session.type").value("TOKEN"))
                .andExpect(jsonPath("$.session.value").value("partner-token"))
                .andExpect(jsonPath("$.expiresAt").doesNotExist())
                .andExpect(jsonPath("$.session.expiresAt").doesNotExist());

        verify(launchService).launch("insurance-app", "kk-KZ");
    }

    @Test
    void returnsApiFirstLaunchWithoutSession() throws Exception {
        when(launchService.launch("insurance-app", "ru")).thenReturn(new LaunchResponse(
                "insurance-app",
                "https://platform.example.com/app",
                null
        ));

        mockMvc.perform(post("/hub/api/v1/applications/insurance-app/launch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session").value(nullValue()));
    }

    @Test
    void returnsReadyViewUrlWithoutDuplicatingItInSession() throws Exception {
        when(launchService.launch("gourmet", "kk-KZ")).thenReturn(new LaunchResponse(
                "gourmet",
                "https://gourmet.example/view?token=abc",
                new LaunchSessionResponse(SessionMode.VIEW_URL, null)
        ));

        mockMvc.perform(post("/hub/api/v1/applications/gourmet/launch")
                        .header("Accept-Language", "kk-KZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryUrl")
                        .value("https://gourmet.example/view?token=abc"))
                .andExpect(jsonPath("$.session.type").value("VIEW_URL"))
                .andExpect(jsonPath("$.session.value").doesNotExist());
    }

    @Test
    void returnsConflictWhenApplicationCannotBeLaunched() throws Exception {
        when(launchService.launch("insurance-app", "ru"))
                .thenThrow(new ApplicationUnavailableException());

        mockMvc.perform(post("/hub/api/v1/applications/insurance-app/launch"))
                .andExpect(status().isConflict())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.code").value("APPLICATION_UNAVAILABLE"));
    }

    @Test
    void hidesPartnerFailureBehindBadGateway() throws Exception {
        when(launchService.launch("insurance-app", "ru"))
                .thenThrow(new PartnerApiUnavailableException());

        mockMvc.perform(post("/hub/api/v1/applications/insurance-app/launch"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("PARTNER_API_UNAVAILABLE"));
    }

    @Test
    void doesNotExposeLegacySessionEndpoints() throws Exception {
        mockMvc.perform(post(
                        "/hub/api/v1/applications/insurance-app/session/token"
                ))
                .andExpect(status().isNotFound());
    }
}
