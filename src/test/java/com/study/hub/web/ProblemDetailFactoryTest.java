package com.study.hub.web;

import com.study.hub.config.ProblemProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailFactoryTest {

    private final ProblemDetailFactory factory = new ProblemDetailFactory(
            new ProblemProperties(URI.create("https://api.example.com/hub/problems/"))
    );

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void createsRfc9457ProblemWithTraceContext() {
        MDC.put(TraceContext.TRACE_ID_MDC_KEY, "trace-123");
        MDC.put(TraceContext.CORRELATION_ID_MDC_KEY, "correlation-456");

        var problem = factory.create(
                HttpStatus.BAD_REQUEST,
                "Validation error",
                "Invalid request",
                ProblemType.VALIDATION,
                "/hub/api/v1/test"
        );

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getType().toString())
                .isEqualTo("https://api.example.com/hub/problems/validation");
        assertThat(problem.getInstance().toString()).isEqualTo("/hub/api/v1/test");
        assertThat(problem.getProperties())
                .containsEntry("code", "VALIDATION_ERROR")
                .containsEntry("traceId", "trace-123")
                .containsEntry("correlationId", "correlation-456");
    }
}
