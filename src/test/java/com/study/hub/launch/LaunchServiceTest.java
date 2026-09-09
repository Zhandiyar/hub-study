package com.study.hub.launch;

import com.study.hub.application.ApplicationAvailabilityService;
import com.study.hub.client.appregistry.dto.HubApplicationConfigurationResponse;
import com.study.hub.client.appregistry.dto.InterfaceConfigurationResponse;
import com.study.hub.client.appregistry.dto.LocalizedTextResponse;
import com.study.hub.client.appregistry.model.ApplicationStatus;
import com.study.hub.client.appregistry.model.IntegrationType;
import com.study.hub.client.appregistry.model.SessionMode;
import com.study.hub.client.partner.PartnerApiClient;
import com.study.hub.exception.AppRegistryUnavailableException;
import com.study.hub.exception.ApplicationUnavailableException;
import com.study.hub.exception.ApplicationNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LaunchServiceTest {

    @Mock
    private ApplicationAvailabilityService applicationAvailabilityService;

    @Mock
    private PartnerApiClient partnerApiClient;

    @Test
    void launchesPartnerWebviewWithToken() {
        when(applicationAvailabilityService.getActiveConfiguration("insurance-app"))
                .thenReturn(configuration(
                        ApplicationStatus.ACTIVE,
                        IntegrationType.PARTNER_WEBVIEW,
                        SessionMode.TOKEN
                ));
        when(partnerApiClient.createSessionToken("insurance-app"))
                .thenReturn("partner-token");

        LaunchResponse response = service().launch("insurance-app");

        assertThat(response.appId()).isEqualTo("insurance-app");
        assertThat(response.entryUrl()).isEqualTo("https://partner.example.com/app");
        assertThat(response.session().type()).isEqualTo(SessionMode.TOKEN);
        assertThat(response.session().value()).isEqualTo("partner-token");
        verify(partnerApiClient).createSessionToken("insurance-app");
    }

    @Test
    void launchesPartnerWebviewWithCode() {
        when(applicationAvailabilityService.getActiveConfiguration("insurance-app"))
                .thenReturn(configuration(
                        ApplicationStatus.ACTIVE,
                        IntegrationType.PARTNER_WEBVIEW,
                        SessionMode.CODE
                ));
        when(partnerApiClient.createSessionCode("insurance-app"))
                .thenReturn("123456");

        LaunchResponse response = service().launch("insurance-app");

        assertThat(response.session().type()).isEqualTo(SessionMode.CODE);
        assertThat(response.session().value()).isEqualTo("123456");
        verify(partnerApiClient).createSessionCode("insurance-app");
    }

    @Test
    void launchesApiFirstWithoutPartnerSession() {
        when(applicationAvailabilityService.getActiveConfiguration("insurance-app"))
                .thenReturn(configuration(
                        ApplicationStatus.ACTIVE,
                        IntegrationType.API_FIRST,
                        SessionMode.NONE
                ));

        LaunchResponse response = service().launch("insurance-app");

        assertThat(response.entryUrl()).isEqualTo("https://partner.example.com/app");
        assertThat(response.session()).isNull();
        verifyNoInteractions(partnerApiClient);
    }

    @Test
    void doesNotCallPartnerWhenApplicationIsUnavailable() {
        when(applicationAvailabilityService.getActiveConfiguration("insurance-app"))
                .thenThrow(new ApplicationUnavailableException());

        assertThatThrownBy(() -> service().launch("insurance-app"))
                .isInstanceOf(ApplicationUnavailableException.class);

        verifyNoInteractions(partnerApiClient);
    }

    @Test
    void rejectsIncompatibleRegistryConfiguration() {
        when(applicationAvailabilityService.getActiveConfiguration("insurance-app"))
                .thenReturn(configuration(
                        ApplicationStatus.ACTIVE,
                        IntegrationType.API_FIRST,
                        SessionMode.TOKEN
                ));

        assertThatThrownBy(() -> service().launch("insurance-app"))
                .isInstanceOf(AppRegistryUnavailableException.class);

        verifyNoInteractions(partnerApiClient);
    }

    @Test
    void doesNotCallPartnerForUnknownApplication() {
        when(applicationAvailabilityService.getActiveConfiguration("unknown-app"))
                .thenThrow(new ApplicationNotFoundException());

        assertThatThrownBy(() -> service().launch("unknown-app"))
                .isInstanceOf(ApplicationNotFoundException.class);

        verifyNoInteractions(partnerApiClient);
    }

    private LaunchService service() {
        return new LaunchService(applicationAvailabilityService, partnerApiClient);
    }

    private HubApplicationConfigurationResponse configuration(
            ApplicationStatus status,
            IntegrationType integrationType,
            SessionMode sessionMode
    ) {
        return new HubApplicationConfigurationResponse(
                "insurance-app",
                "insurance",
                status,
                null,
                1,
                integrationType,
                sessionMode,
                new InterfaceConfigurationResponse(
                        "https://partner.example.com/app",
                        title(),
                        null,
                        10
                ),
                null,
                null
        );
    }

    private LocalizedTextResponse title() {
        return new LocalizedTextResponse("Страхование", "Сақтандыру", "Insurance");
    }
}
