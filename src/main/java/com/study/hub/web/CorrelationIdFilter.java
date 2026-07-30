package com.study.hub.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final int MAX_ID_LENGTH = 128;
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._:-]+");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = firstValid(
                request.getHeader(TraceContext.TRACE_ID_HEADER),
                request.getHeader(TraceContext.CORRELATION_ID_HEADER)
        );
        String correlationId = validOrDefault(
                request.getHeader(TraceContext.CORRELATION_ID_HEADER),
                traceId
        );

        MDC.put(TraceContext.TRACE_ID_MDC_KEY, traceId);
        MDC.put(TraceContext.CORRELATION_ID_MDC_KEY, correlationId);
        response.setHeader(TraceContext.TRACE_ID_HEADER, traceId);
        response.setHeader(TraceContext.CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TraceContext.TRACE_ID_MDC_KEY);
            MDC.remove(TraceContext.CORRELATION_ID_MDC_KEY);
        }
    }

    private String firstValid(String first, String second) {
        if (isValid(first)) {
            return first;
        }
        if (isValid(second)) {
            return second;
        }
        return UUID.randomUUID().toString();
    }

    private String validOrDefault(String value, String defaultValue) {
        return isValid(value) ? value : defaultValue;
    }

    private boolean isValid(String value) {
        return value != null
                && !value.isBlank()
                && value.length() <= MAX_ID_LENGTH
                && SAFE_ID.matcher(value).matches();
    }
}
