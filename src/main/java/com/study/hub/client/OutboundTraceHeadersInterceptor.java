package com.study.hub.client;

import com.study.hub.web.TraceContext;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OutboundTraceHeadersInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        copyMdcHeader(request, TraceContext.TRACE_ID_HEADER, TraceContext.TRACE_ID_MDC_KEY);
        copyMdcHeader(
                request,
                TraceContext.CORRELATION_ID_HEADER,
                TraceContext.CORRELATION_ID_MDC_KEY
        );
        return execution.execute(request, body);
    }

    private void copyMdcHeader(HttpRequest request, String headerName, String mdcKey) {
        String value = MDC.get(mdcKey);
        if (value != null && !value.isBlank()) {
            request.getHeaders().set(headerName, value);
        }
    }
}
