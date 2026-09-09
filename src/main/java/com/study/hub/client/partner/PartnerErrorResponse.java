package com.study.hub.client.partner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
record PartnerErrorResponse(
        String message,

        @JsonProperty("error_code")
        String errorCode,

        Map<String, List<String>> errors
) {

    static PartnerErrorResponse empty() {
        return new PartnerErrorResponse(null, null, null);
    }

    String detailOr(String fallback) {
        return message == null || message.isBlank() ? fallback : message;
    }
}
