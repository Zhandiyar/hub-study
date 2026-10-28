package com.study.hub.client.partner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.hub.exception.ContractNotFoundException;
import com.study.hub.exception.PartnerApiUnavailableException;
import com.study.hub.exception.PartnerBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PartnerApiClientTest {

    private MockRestServiceServer server;
    private PartnerApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new PartnerApiClient(
                builder.build(),
                new PartnerErrorParser(new ObjectMapper())
        );
    }

    @Test
    void returnsNormalizedPartnerContract() {
        server.expect(requestTo(
                        "http://insurance-app/internal/api/v1/contracts/contract-123"
                ))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "contractNumber": "contract-123",
                          "status": "ACTIVE",
                          "amount": 15000.00,
                          "currency": "KZT",
                          "paymentFields": {
                            "type": "flight"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        PartnerContractResponse result = client.getContract(
                "insurance-app",
                "contract-123"
        );

        assertThat(result.contractNumber()).isEqualTo("contract-123");
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.amount()).isEqualByComparingTo("15000.00");
        assertThat(result.currency()).isEqualTo("KZT");
        assertThat(result.paymentFields()).containsEntry("type", "flight");
        server.verify();
    }

    @Test
    void mapsMissingContractToNotFound() {
        server.expect(requestTo(
                        "http://insurance-app/internal/api/v1/contracts/missing"
                ))
                .andRespond(withResourceNotFound());

        assertThatThrownBy(() -> client.getContract("insurance-app", "missing"))
                .isInstanceOf(ContractNotFoundException.class);
        server.verify();
    }

    @Test
    void hidesPartnerServerError() {
        server.expect(requestTo(
                        "http://insurance-app/internal/api/v1/contracts/contract-123"
                ))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "accountNumber": "sensitive-account-number"
                                }
                                """));

        assertThatThrownBy(() -> client.getContract(
                "insurance-app",
                "contract-123"
        ))
                .isInstanceOf(PartnerApiUnavailableException.class)
                .hasMessage("Partner service is temporarily unavailable")
                .hasNoCause();
        server.verify();
    }

    @Test
    void mapsPartnerUnauthorizedToUnavailable() {
        server.expect(requestTo(
                        "http://insurance-app/internal/api/v1/contracts/contract-123"
                ))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.getContract(
                "insurance-app",
                "contract-123"
        ))
                .isInstanceOf(PartnerApiUnavailableException.class)
                .hasMessage("Partner service is temporarily unavailable")
                .hasNoCause();
        server.verify();
    }

    @Test
    void mapsPartnerBusinessError() {
        server.expect(requestTo(
                        "http://insurance-app/internal/api/v1/contracts/contract-123"
                ))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "message": "Contract amount is invalid",
                                  "error_code": "CONTRACT_DIFFERENT_AMOUNT",
                                  "errors": {
                                    "contract": ["Contract amount is invalid"]
                                  }
                                }
                                """));

        assertThatThrownBy(() -> client.getContract(
                "insurance-app",
                "contract-123"
        )).isInstanceOfSatisfying(PartnerBusinessException.class, exception -> {
            assertThat(exception.getMessage()).isEqualTo("Contract amount is invalid");
            assertThat(exception.errorCode()).isEqualTo("CONTRACT_DIFFERENT_AMOUNT");
            assertThat(exception.errors()).containsKey("contract");
        });
        server.verify();
    }

    @Test
    void rejectsEmptySuccessfulResponse() {
        server.expect(requestTo(
                        "http://insurance-app/internal/api/v1/contracts/contract-123"
                ))
                .andRespond(withSuccess());

        assertThatThrownBy(() -> client.getContract(
                "insurance-app",
                "contract-123"
        ))
                .isInstanceOf(PartnerApiUnavailableException.class);
        server.verify();
    }

    @Test
    void rejectsJsonWithoutRequiredContractFields() {
        server.expect(requestTo(
                        "http://insurance-app/internal/api/v1/contracts/contract-123"
                ))
                .andRespond(withSuccess("""
                        {
                          "contractNumber": "contract-123",
                          "status": "ACTIVE"
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getContract("insurance-app", "contract-123"))
                .isInstanceOf(PartnerApiUnavailableException.class);
        server.verify();
    }

    @Test
    void rejectsContractNumberDifferentFromRequested() {
        server.expect(requestTo(
                        "http://insurance-app/internal/api/v1/contracts/contract-123"
                ))
                .andRespond(withSuccess("""
                        {
                          "contractNumber": "another-contract",
                          "status": "ACTIVE",
                          "amount": 100.00,
                          "currency": "KZT"
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getContract("insurance-app", "contract-123"))
                .isInstanceOf(PartnerApiUnavailableException.class);
        server.verify();
    }

    @Test
    void createsSessionToken() {
        server.expect(requestTo(
                        "http://insurance-app/internal/api/v1/session/token"
                ))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"token\":\"partner-session-token\"}",
                        MediaType.APPLICATION_JSON
                ));

        String result = client.createSessionToken("insurance-app");

        assertThat(result).isEqualTo("partner-session-token");
        server.verify();
    }

    @Test
    void createsSessionCode() {
        server.expect(requestTo(
                        "http://insurance-app/internal/api/v1/session/code"
                ))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"code\":\"123456\"}",
                        MediaType.APPLICATION_JSON
                ));

        String result = client.createSessionCode("insurance-app");

        assertThat(result).isEqualTo("123456");
        server.verify();
    }

    @Test
    void rejectsBlankSessionValue() {
        server.expect(requestTo(
                        "http://insurance-app/internal/api/v1/session/token"
                ))
                .andRespond(withSuccess("{\"token\":\" \"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.createSessionToken("insurance-app"))
                .isInstanceOf(PartnerApiUnavailableException.class);
        server.verify();
    }

    @Test
    void mapsSessionServerErrorWithoutExposingResponseBody() {
        server.expect(requestTo(
                        "http://insurance-app/internal/api/v1/session/code"
                ))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"sensitive-code\"}"));

        assertThatThrownBy(() -> client.createSessionCode("insurance-app"))
                .isInstanceOf(PartnerApiUnavailableException.class)
                .hasMessage("Partner service is temporarily unavailable")
                .hasNoCause();
        server.verify();
    }

    @Test
    void mapsSessionBusinessError() {
        server.expect(requestTo(
                        "http://insurance-app/internal/api/v1/session/token"
                ))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "message": "Session cannot be created",
                                  "error_code": "SESSION_NOT_AVAILABLE"
                                }
                                """));

        assertThatThrownBy(() -> client.createSessionToken("insurance-app"))
                .isInstanceOfSatisfying(PartnerBusinessException.class, exception -> {
                    assertThat(exception.getMessage()).isEqualTo("Session cannot be created");
                    assertThat(exception.errorCode()).isEqualTo("SESSION_NOT_AVAILABLE");
                });
        server.verify();
    }
}
