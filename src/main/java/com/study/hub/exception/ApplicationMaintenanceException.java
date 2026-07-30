package com.study.hub.exception;

public class ApplicationMaintenanceException extends RuntimeException {

    private static final String DEFAULT_MESSAGE =
            "Partner application is temporarily unavailable due to maintenance";

    public ApplicationMaintenanceException(String maintenanceMessage) {
        super(maintenanceMessage == null || maintenanceMessage.isBlank()
                ? DEFAULT_MESSAGE
                : maintenanceMessage);
    }
}
