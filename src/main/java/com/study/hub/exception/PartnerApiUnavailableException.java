package com.study.hub.exception;

public class PartnerApiUnavailableException extends RuntimeException {

    public PartnerApiUnavailableException() {
        super("Partner service is temporarily unavailable");
    }

    public PartnerApiUnavailableException(Throwable cause) {
        super("Partner service is temporarily unavailable", cause);
    }
}
