package com.study.hub.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void documentsResponsesForEveryHubApi() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode openApi = objectMapper.readTree(body);

        JsonNode applicationOperation = operation(
                openApi,
                "/hub/api/v1/applications/{appId}"
        );
        assertResponses(
                applicationOperation,
                "200", "400", "401", "404", "500", "503"
        );
        assertResponseSchema(
                applicationOperation,
                "ApplicationConfigurationResponse"
        );
        JsonNode applicationProperties = openApi.path("components")
                .path("schemas")
                .path("ApplicationConfigurationResponse")
                .path("properties");
        assertNullable(applicationProperties, "maintenanceMessages");
        assertNullable(applicationProperties, "interfaceConfiguration");
        assertNullable(applicationProperties, "nativeSecurityConfiguration");
        assertNullable(applicationProperties, "financialConfiguration");
        JsonNode financialProperties = openApi.path("components")
                .path("schemas")
                .path("ApplicationFinancialConfigurationResponse")
                .path("properties");
        assertThat(financialProperties.has("serviceId")).isTrue();
        assertThat(financialProperties.has("paymentWebhookUrl")).isFalse();
        assertThat(financialProperties.has("credentialId")).isFalse();

        JsonNode catalogOperation = operation(openApi, "/hub/api/v1/catalog");
        assertResponses(
                catalogOperation,
                "200", "400", "401", "404", "500", "503"
        );
        assertResponseSchema(
                catalogOperation,
                "CatalogGroupResponse"
        );
        JsonNode catalogSchema = openApi.path("components")
                .path("schemas")
                .path("CatalogGroupResponse");
        assertThat(catalogSchema.path("properties").has("items")).isTrue();
        assertThat(catalogSchema.path("properties").has("id")).isTrue();
        assertThat(catalogSchema.path("properties").has("groupId")).isFalse();
        JsonNode catalogApplicationSchema = openApi.path("components")
                .path("schemas")
                .path("CatalogApplicationResponse")
                .path("allOf")
                .get(1)
                .path("properties");
        assertThat(catalogApplicationSchema.has("id")).isTrue();
        assertThat(catalogApplicationSchema.has("appId")).isFalse();
        assertResponses(
                operation(
                        openApi,
                        "/hub/api/v1/services/{serviceId}/contracts/{contractNumber}"
                ),
                "200", "400", "401", "404", "409", "422", "500", "502", "503"
        );
        JsonNode launchOperation = operation(
                openApi,
                "/hub/api/v1/applications/{appId}/launch",
                "post"
        );
        assertResponses(
                launchOperation,
                "200", "400", "401", "404", "409", "422", "500", "502", "503"
        );
        assertResponseSchema(launchOperation, "LaunchResponse");

        JsonNode problemProperties = openApi.path("components")
                .path("schemas")
                .path("ProblemDetail")
                .path("properties");
        assertThat(problemProperties.has("code")).isTrue();
        assertThat(problemProperties.has("errorCode")).isTrue();
        assertThat(problemProperties.has("errors")).isTrue();
        assertThat(problemProperties.has("traceId")).isTrue();
        assertThat(problemProperties.has("correlationId")).isTrue();
    }

    private JsonNode operation(JsonNode openApi, String path) {
        return operation(openApi, path, "get");
    }

    private JsonNode operation(JsonNode openApi, String path, String method) {
        JsonNode operation = openApi.path("paths").path(path).path(method);
        assertThat(operation.isMissingNode()).isFalse();
        return operation;
    }

    private void assertResponses(JsonNode operation, String... expectedStatuses) {
        JsonNode responses = operation.path("responses");
        for (String status : expectedStatuses) {
            assertThat(responses.has(status))
                    .as("response %s must be documented", status)
                    .isTrue();
        }
    }

    private void assertResponseSchema(JsonNode operation, String schemaName) {
        String schemaReference = operation.path("responses")
                .path("200")
                .path("content")
                .path("application/json")
                .path("schema")
                .path("$ref")
                .asText();
        assertThat(schemaReference)
                .isEqualTo("#/components/schemas/" + schemaName);
    }

    private void assertNullable(JsonNode properties, String propertyName) {
        JsonNode property = properties.path(propertyName);
        JsonNode type = property.path("type");
        boolean nullable = property.path("nullable").asBoolean()
                || type.isTextual() && "null".equals(type.asText())
                || type.isArray() && type.valueStream()
                .anyMatch(value -> "null".equals(value.asText()));

        assertThat(nullable)
                .as("property %s must be nullable", propertyName)
                .isTrue();
    }
}
