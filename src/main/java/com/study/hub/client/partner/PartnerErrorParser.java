package com.study.hub.client.partner;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class PartnerErrorParser {

    private final ObjectMapper objectMapper;

    PartnerErrorResponse parse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return PartnerErrorResponse.empty();
        }

        try {
            PartnerErrorResponse response = objectMapper.readValue(
                    responseBody,
                    PartnerErrorResponse.class
            );
            return response == null ? PartnerErrorResponse.empty() : response;
        } catch (Exception exception) {
            log.debug("Could not parse partner error response as JSON", exception);
            return PartnerErrorResponse.empty();
        }
    }
}
