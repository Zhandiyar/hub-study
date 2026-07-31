package com.study.hub.partnerapp;

import com.study.hub.client.appregistry.AppRegistryApplicationConfiguration;
import com.study.hub.client.appregistry.AppRegistryApplicationGroup;
import com.study.hub.client.appregistry.AppRegistryClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartnerApplicationServiceTest {

    @Mock
    private AppRegistryClient appRegistryClient;

    private final PartnerApplicationMapper mapper =
            Mappers.getMapper(PartnerApplicationMapper.class);

    @Test
    void returnsOnlyClientSafeFieldsForActiveApplication() {
        var service = new PartnerApplicationService(appRegistryClient, mapper);
        when(appRegistryClient.getApplicationConfiguration("insurance-app"))
                .thenReturn(configuration(
                        ApplicationStatus.ACTIVE,
                        null
                ));

        PartnerApplicationResponse result =
                service.getApplication("insurance-app");

        assertThat(result.appId()).isEqualTo("insurance-app");
        assertThat(result.status()).isEqualTo(ApplicationStatus.ACTIVE);
        assertThat(result.sessionMode()).isEqualTo(SessionMode.TOKEN);
        assertThat(result.interfaceConfiguration().entryUrl())
                .isEqualTo("https://partner.example/app");
        assertThat(result.nativeSecurityConfiguration().allowedBridgeMethods())
                .containsExactly("close");
    }

    @Test
    void returnsDisabledStatusToFrontend() {
        var service = new PartnerApplicationService(appRegistryClient, mapper);
        when(appRegistryClient.getApplicationConfiguration("insurance-app"))
                .thenReturn(configuration(
                        ApplicationStatus.DISABLED,
                        null
                ));

        PartnerApplicationResponse result = service.getApplication("insurance-app");

        assertThat(result.status()).isEqualTo(ApplicationStatus.DISABLED);
    }

    @Test
    void returnsMaintenanceStatusAndAllTranslationsToFrontend() {
        var service = new PartnerApplicationService(appRegistryClient, mapper);
        when(appRegistryClient.getApplicationConfiguration("insurance-app"))
                .thenReturn(configuration(
                        ApplicationStatus.MAINTENANCE,
                        maintenanceMessages()
                ));

        PartnerApplicationResponse result = service.getApplication("insurance-app");

        assertThat(result.status()).isEqualTo(ApplicationStatus.MAINTENANCE);
        assertThat(result.maintenanceMessages().ru())
                .isEqualTo("Технические работы до 18:00");
        assertThat(result.maintenanceMessages().kk())
                .isEqualTo("Техникалық жұмыстар сағат 18:00-ге дейін");
        assertThat(result.maintenanceMessages().en())
                .isEqualTo("Maintenance until 18:00");
    }

    @Test
    void returnsGroupWithoutInternalFinancialConfiguration() {
        var service = new PartnerApplicationService(appRegistryClient, mapper);
        when(appRegistryClient.getApplicationGroup("insurance", 0, 20))
                .thenReturn(new AppRegistryApplicationGroup(
                        "insurance",
                        "Страхование",
                        1,
                        List.of(configuration(ApplicationStatus.ACTIVE, null)),
                        0,
                        20,
                        1,
                        1
                ));

        PartnerApplicationGroupResponse result =
                service.getApplicationGroup("insurance", 0, 20);

        assertThat(result.groupId()).isEqualTo("insurance");
        assertThat(result.applications()).hasSize(1);
        assertThat(result.applications().getFirst().appId())
                .isEqualTo("insurance-app");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    private AppRegistryApplicationConfiguration configuration(
            ApplicationStatus status,
            AppRegistryApplicationConfiguration.MaintenanceMessages maintenanceMessages
    ) {
        return new AppRegistryApplicationConfiguration(
                "insurance-app",
                "insurance",
                status,
                maintenanceMessages,
                3,
                SessionMode.TOKEN,
                new AppRegistryApplicationConfiguration.InterfaceConfiguration(
                        "https://partner.example/app",
                        "Страхование"
                ),
                new AppRegistryApplicationConfiguration.NativeSecurityConfiguration(
                        List.of("partner.example"),
                        "default-src 'self'",
                        List.of("close")
                ),
                new AppRegistryApplicationConfiguration.FinancialConfiguration(
                        "merchant-1",
                        "https://partner.example/webhook",
                        "partner-app-credential"
                )
        );
    }

    private AppRegistryApplicationConfiguration.MaintenanceMessages maintenanceMessages() {
        return new AppRegistryApplicationConfiguration.MaintenanceMessages(
                "Технические работы до 18:00",
                "Техникалық жұмыстар сағат 18:00-ге дейін",
                "Maintenance until 18:00"
        );
    }
}
