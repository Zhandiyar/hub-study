package com.study.hub.exception;

import java.util.List;
import java.util.Map;

public class PartnerBusinessException extends RuntimeException {

    private final String errorCode;
    private final Map<String, List<String>> errors;

    public PartnerBusinessException(
            String message,
            String errorCode,
            Map<String, List<String>> errors
    ) {
        super(message);
        this.errorCode = errorCode;
        this.errors = errors;
    }

    public String errorCode() {
        return errorCode;
    }

    public Map<String, List<String>> errors() {
        return errors;
    }
}
