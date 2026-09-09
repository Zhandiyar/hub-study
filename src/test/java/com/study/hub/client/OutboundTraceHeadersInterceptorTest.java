package com.study.hub.client;

import com.study.hub.web.TraceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutboundTraceHeadersInterceptorTest {

    private final OutboundTraceHeadersInterceptor interceptor =
            new OutboundTraceHeadersInterceptor();

    @AfterEach
    void clearMdc() {
        MDC.remove(TraceContext.TRACE_ID_MDC_KEY);
        MDC.remove(TraceContext.CORRELATION_ID_MDC_KEY);
    }

    @Test
    void propagatesTraceContext() throws Exception {
        MDC.put(TraceContext.TRACE_ID_MDC_KEY, "trace-123");
        MDC.put(TraceContext.CORRELATION_ID_MDC_KEY, "operation-456");
        HttpHeaders headers = new HttpHeaders();
        HttpRequest request = mock(HttpRequest.class);
        when(request.getHeaders()).thenReturn(headers);
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(execution.execute(eq(request), any(byte[].class))).thenReturn(response);

        ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

        assertThat(headers.getFirst(TraceContext.TRACE_ID_HEADER)).isEqualTo("trace-123");
        assertThat(headers.getFirst(TraceContext.CORRELATION_ID_HEADER))
                .isEqualTo("operation-456");
        assertThat(result).isSameAs(response);
    }

    @Test
    void doesNotAddEmptyTraceHeaders() throws Exception {
        MDC.put(TraceContext.TRACE_ID_MDC_KEY, " ");
        HttpHeaders headers = new HttpHeaders();
        HttpRequest request = mock(HttpRequest.class);
        when(request.getHeaders()).thenReturn(headers);
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(execution.execute(eq(request), any(byte[].class))).thenReturn(response);

        ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

        assertThat(headers.containsKey(TraceContext.TRACE_ID_HEADER)).isFalse();
        assertThat(headers.containsKey(TraceContext.CORRELATION_ID_HEADER)).isFalse();
        assertThat(result).isSameAs(response);
    }
}
