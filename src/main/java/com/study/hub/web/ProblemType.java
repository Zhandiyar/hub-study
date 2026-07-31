package com.study.hub.web;

public enum ProblemType {

    VALIDATION("validation", "VALIDATION_ERROR"),
    UNAUTHORIZED("unauthorized", "UNAUTHORIZED"),
    FORBIDDEN("forbidden", "FORBIDDEN"),
    MALFORMED_REQUEST("malformed-request", "MALFORMED_REQUEST"),
    APPLICATION_NOT_FOUND("application-not-found", "APPLICATION_NOT_FOUND"),
    DEPENDENCY_UNAVAILABLE("dependency-unavailable", "DEPENDENCY_UNAVAILABLE"),
    INTERNAL("internal", "INTERNAL_ERROR");

    private final String id;
    private final String code;

    ProblemType(String id, String code) {
        this.id = id;
        this.code = code;
    }

    public String id() {
        return id;
    }

    public String code() {
        return code;
    }
}
