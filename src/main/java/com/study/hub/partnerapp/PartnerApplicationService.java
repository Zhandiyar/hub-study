package com.study.hub.partnerapp;

import com.study.hub.client.appregistry.AppRegistryApplicationConfiguration;
import com.study.hub.client.appregistry.AppRegistryClient;
import com.study.hub.exception.ApplicationMaintenanceException;
import com.study.hub.exception.ApplicationNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PartnerApplicationService {

    private final AppRegistryClient appRegistryClient;
    private final PartnerApplicationMapper mapper;

    public PartnerApplicationResponse getApplication(String appId, Locale locale) {
        AppRegistryApplicationConfiguration configuration =
                appRegistryClient.getApplicationConfiguration(appId);

        return switch (configuration.status()) {
            case ACTIVE -> mapper.toResponse(configuration);
            case DISABLED -> throw new ApplicationNotFoundException();
            case MAINTENANCE -> throw new ApplicationMaintenanceException(
                    resolveMaintenanceMessage(configuration.maintenanceMessages(), locale)
            );
        };
    }

    private String resolveMaintenanceMessage(
            AppRegistryApplicationConfiguration.MaintenanceMessages messages,
            Locale locale
    ) {
        if (messages == null) {
            return null;
        }

        String language = locale == null ? "" : locale.getLanguage();
        return switch (language) {
            case "kk" -> messages.kk();
            case "en" -> messages.en();
            default -> messages.ru();
        };
    }
}
