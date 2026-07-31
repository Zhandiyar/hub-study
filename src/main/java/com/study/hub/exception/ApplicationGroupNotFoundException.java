package com.study.hub.exception;

public class ApplicationGroupNotFoundException extends RuntimeException {

    public ApplicationGroupNotFoundException() {
        super("Partner application group was not found");
    }
}
