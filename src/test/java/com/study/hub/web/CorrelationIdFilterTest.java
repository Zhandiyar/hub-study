package com.study.hub.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void propagatesValidTraceAndCorrelationIds() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(TraceContext.TRACE_ID_HEADER, "trace-123");
        request.addHeader(TraceContext.CORRELATION_ID_HEADER, "correlation-456");
        var response = new MockHttpServletResponse();

        FilterChain filterChain = (servletRequest, servletResponse) -> {
            assertThat(MDC.get(TraceContext.TRACE_ID_MDC_KEY)).isEqualTo("trace-123");
            assertThat(MDC.get(TraceContext.CORRELATION_ID_MDC_KEY))
                    .isEqualTo("correlation-456");
        };

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader(TraceContext.TRACE_ID_HEADER))
                .isEqualTo("trace-123");
        assertThat(response.getHeader(TraceContext.CORRELATION_ID_HEADER))
                .isEqualTo("correlation-456");
        assertThat(MDC.get(TraceContext.TRACE_ID_MDC_KEY)).isNull();
        assertThat(MDC.get(TraceContext.CORRELATION_ID_MDC_KEY)).isNull();
    }

    @Test
    void replacesUnsafeIncomingIds() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(TraceContext.TRACE_ID_HEADER, "unsafe\nvalue");
        request.addHeader(TraceContext.CORRELATION_ID_HEADER, "also unsafe");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
        });

        assertThat(response.getHeader(TraceContext.TRACE_ID_HEADER))
                .matches("[0-9a-f-]{36}");
        assertThat(response.getHeader(TraceContext.CORRELATION_ID_HEADER))
                .isEqualTo(response.getHeader(TraceContext.TRACE_ID_HEADER));
    }
}
