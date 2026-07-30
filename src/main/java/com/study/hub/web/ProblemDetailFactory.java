package com.study.hub.web;

import com.study.hub.config.ProblemProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProblemDetailFactory {

    private final ProblemProperties properties;

    public ProblemDetail create(
            HttpStatus status,
            String title,
            String detail,
            ProblemType type,
            String requestUri
    ) {
        return create(status, title, detail, type, requestUri, null);
    }

    public ProblemDetail create(
            HttpStatus status,
            String title,
            String detail,
            ProblemType type,
            String requestUri,
            Map<String, List<String>> errors
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(properties.baseUri().resolve(type.id()));
        problem.setInstance(URI.create(requestUri));
        problem.setProperty("code", type.code());

        if (errors != null && !errors.isEmpty()) {
            problem.setProperty("errors", errors);
        }

        putMdcValue(problem, "traceId", TraceContext.TRACE_ID_MDC_KEY);
        putMdcValue(problem, "correlationId", TraceContext.CORRELATION_ID_MDC_KEY);
        return problem;
    }

    private void putMdcValue(ProblemDetail problem, String property, String mdcKey) {
        String value = MDC.get(mdcKey);
        if (value != null && !value.isBlank()) {
            problem.setProperty(property, value);
        }
    }
}
