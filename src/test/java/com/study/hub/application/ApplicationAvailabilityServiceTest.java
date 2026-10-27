package com.study.hub.application;

import com.study.hub.client.appregistry.AppRegistryClient;
import com.study.hub.client.appregistry.dto.HubApplicationConfigurationResponse;
import com.study.hub.client.appregistry.dto.ApplicationRoutingResponse;
import com.study.hub.client.appregistry.model.ApplicationStatus;
import com.study.hub.exception.ApplicationUnavailableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationAvailabilityServiceTest {

    @Mock
    private AppRegistryClient appRegistryClient;

    @Test
    void returnsActiveConfiguration() {
        HubApplicationConfigurationResponse configuration = configuration(
                ApplicationStatus.ACTIVE
        );
        when(appRegistryClient.getApplicationConfiguration("insurance-app"))
                .thenReturn(configuration);

        HubApplicationConfigurationResponse result = service()
                .getActiveConfiguration("insurance-app");

        assertThat(result).isSameAs(configuration);
    }

    @ParameterizedTest
    @EnumSource(
            value = ApplicationStatus.class,
            names = {"DISABLED", "MAINTENANCE"}
    )
    void rejectsUnavailableStatus(ApplicationStatus status) {
        when(appRegistryClient.getApplicationConfiguration("insurance-app"))
                .thenReturn(configuration(status));

        assertThatThrownBy(() -> service().getActiveConfiguration("insurance-app"))
                .isInstanceOf(ApplicationUnavailableException.class);
    }

    @Test
    void returnsActiveRouting() {
        ApplicationRoutingResponse routing = new ApplicationRoutingResponse(
                "insurance",
                "insurance-app",
                ApplicationStatus.ACTIVE
        );
        when(appRegistryClient.getRoutingByServiceId("service-456"))
                .thenReturn(routing);

        ApplicationRoutingResponse result = service()
                .getActiveRoutingByServiceId("service-456");

        assertThat(result).isSameAs(routing);
    }

    @ParameterizedTest
    @EnumSource(
            value = ApplicationStatus.class,
            names = {"DISABLED", "MAINTENANCE"}
    )
    void rejectsUnavailableRoutingStatus(ApplicationStatus status) {
        when(appRegistryClient.getRoutingByServiceId("service-456"))
                .thenReturn(new ApplicationRoutingResponse(
                        "insurance",
                        "insurance-app",
                        status
                ));

        assertThatThrownBy(() -> service()
                .getActiveRoutingByServiceId("service-456"))
                .isInstanceOf(ApplicationUnavailableException.class);
    }

    private ApplicationAvailabilityService service() {
        return new ApplicationAvailabilityService(appRegistryClient);
    }

    private HubApplicationConfigurationResponse configuration(ApplicationStatus status) {
        return new HubApplicationConfigurationResponse(
                "insurance-app",
                "insurance",
                status,
                null,
                1,
                null,
                null,
                null,
                null,
                null
        );
    }
}
