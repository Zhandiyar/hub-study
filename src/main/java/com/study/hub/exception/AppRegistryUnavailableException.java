package com.study.hub.exception;

public class AppRegistryUnavailableException extends RuntimeException {

    public AppRegistryUnavailableException() {
        super("App-registry is temporarily unavailable");
    }

    public AppRegistryUnavailableException(Throwable cause) {
        super("App-registry is temporarily unavailable", cause);
    }
}
