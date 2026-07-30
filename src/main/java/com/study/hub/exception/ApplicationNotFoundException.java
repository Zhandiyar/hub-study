package com.study.hub.exception;

public class ApplicationNotFoundException extends RuntimeException {

    public ApplicationNotFoundException() {
        super("Partner application was not found");
    }
}
