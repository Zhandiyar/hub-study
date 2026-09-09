package com.study.hub.web;

public enum ProblemType {

    VALIDATION("validation", "VALIDATION_ERROR"),
    MALFORMED_REQUEST("malformed-request", "MALFORMED_REQUEST"),
    APPLICATION_NOT_FOUND("application-not-found", "APPLICATION_NOT_FOUND"),
    CONTRACT_NOT_FOUND("contract-not-found", "CONTRACT_NOT_FOUND"),
    APPLICATION_UNAVAILABLE(
            "application-unavailable",
            "APPLICATION_UNAVAILABLE"
    ),
    PARTNER_BUSINESS_ERROR("partner-business-error", "PARTNER_BUSINESS_ERROR"),
    PARTNER_API_UNAVAILABLE("partner-api-unavailable", "PARTNER_API_UNAVAILABLE"),
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
