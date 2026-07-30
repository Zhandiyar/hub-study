package com.study.hub.partnerapp;

import com.study.hub.client.appregistry.AppRegistryApplicationConfiguration;
import com.study.hub.client.appregistry.AppRegistryClient;
import com.study.hub.exception.ApplicationMaintenanceException;
import com.study.hub.exception.ApplicationNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
                        AppRegistryApplicationConfiguration.ApplicationStatus.ACTIVE,
                        null
                ));

        PartnerApplicationResponse result =
                service.getApplication("insurance-app", Locale.forLanguageTag("ru"));

        assertThat(result.appId()).isEqualTo("insurance-app");
        assertThat(result.sessionMode()).isEqualTo(SessionMode.TOKEN);
        assertThat(result.interfaceConfiguration().entryUrl())
                .isEqualTo("https://partner.example/app");
        assertThat(result.nativeSecurityConfiguration().allowedBridgeMethods())
                .containsExactly("close");
    }

    @Test
    void hidesDisabledApplicationAsNotFound() {
        var service = new PartnerApplicationService(appRegistryClient, mapper);
        when(appRegistryClient.getApplicationConfiguration("insurance-app"))
                .thenReturn(configuration(
                        AppRegistryApplicationConfiguration.ApplicationStatus.DISABLED,
                        null
                ));

        assertThatThrownBy(() -> service.getApplication(
                "insurance-app",
                Locale.forLanguageTag("ru")
        ))
                .isInstanceOf(ApplicationNotFoundException.class);
    }

    @Test
    void returnsKazakhMaintenanceMessageAsServiceUnavailableError() {
        var service = new PartnerApplicationService(appRegistryClient, mapper);
        when(appRegistryClient.getApplicationConfiguration("insurance-app"))
                .thenReturn(configuration(
                        AppRegistryApplicationConfiguration.ApplicationStatus.MAINTENANCE,
                        maintenanceMessages()
                ));

        assertThatThrownBy(() -> service.getApplication(
                "insurance-app",
                Locale.forLanguageTag("kk")
        ))
                .isInstanceOf(ApplicationMaintenanceException.class)
                .hasMessage("Техникалық жұмыстар сағат 18:00-ге дейін");
    }

    @Test
    void usesRussianMaintenanceMessageForUnsupportedLanguage() {
        var service = new PartnerApplicationService(appRegistryClient, mapper);
        when(appRegistryClient.getApplicationConfiguration("insurance-app"))
                .thenReturn(configuration(
                        AppRegistryApplicationConfiguration.ApplicationStatus.MAINTENANCE,
                        maintenanceMessages()
                ));

        assertThatThrownBy(() -> service.getApplication(
                "insurance-app",
                Locale.FRENCH
        ))
                .isInstanceOf(ApplicationMaintenanceException.class)
                .hasMessage("Технические работы до 18:00");
    }

    private AppRegistryApplicationConfiguration configuration(
            AppRegistryApplicationConfiguration.ApplicationStatus status,
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
