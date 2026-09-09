package com.study.hub.exception;

public class ApplicationUnavailableException extends RuntimeException {

    public ApplicationUnavailableException() {
        super("Partner application is not available");
    }
}
