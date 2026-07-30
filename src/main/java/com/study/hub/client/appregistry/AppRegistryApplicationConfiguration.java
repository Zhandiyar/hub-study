package com.study.hub.client.appregistry;

import com.study.hub.partnerapp.SessionMode;

import java.util.List;

public record AppRegistryApplicationConfiguration(
        String appId,
        String groupId,
        ApplicationStatus status,
        MaintenanceMessages maintenanceMessages,
        long version,
        SessionMode sessionMode,
        InterfaceConfiguration interfaceConfiguration,
        NativeSecurityConfiguration nativeSecurityConfiguration,
        FinancialConfiguration financialConfiguration
) {

    public enum ApplicationStatus {
        ACTIVE,
        DISABLED,
        MAINTENANCE
    }

    public record MaintenanceMessages(
            String ru,
            String kk,
            String en
    ) {
    }

    public record InterfaceConfiguration(
            String entryUrl,
            String title
    ) {
    }

    public record NativeSecurityConfiguration(
            List<String> allowedDomains,
            String contentSecurityPolicy,
            List<String> allowedBridgeMethods
    ) {
    }

    public record FinancialConfiguration(
            String merchantId,
            String paymentWebhookUrl,
            String credentialId
    ) {
    }
}
