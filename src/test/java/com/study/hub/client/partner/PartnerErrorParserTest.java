package com.study.hub.client.partner;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PartnerErrorParserTest {

    private final PartnerErrorParser parser = new PartnerErrorParser(new ObjectMapper());

    @Test
    void parsesBusinessError() {
        PartnerErrorResponse response = parser.parse("""
                {
                  "message": "Contract amount is invalid",
                  "error_code": "CONTRACT_DIFFERENT_AMOUNT",
                  "errors": {
                    "contract": ["Contract amount is invalid"]
                  },
                  "code": 422
                }
                """);

        assertThat(response.message()).isEqualTo("Contract amount is invalid");
        assertThat(response.errorCode()).isEqualTo("CONTRACT_DIFFERENT_AMOUNT");
        assertThat(response.errors()).containsEntry(
                "contract",
                List.of("Contract amount is invalid")
        );
    }

    @Test
    void returnsEmptyResponseForMalformedBody() {
        PartnerErrorResponse response = parser.parse("not-json");

        assertThat(response.message()).isNull();
        assertThat(response.errorCode()).isNull();
        assertThat(response.errors()).isNull();
    }
}
