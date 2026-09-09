package com.study.hub.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthorizationHeaderRelayInterceptorTest {

    private final AuthorizationHeaderRelayInterceptor interceptor =
            new AuthorizationHeaderRelayInterceptor();

    @AfterEach
    void resetRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void relaysAuthorizationHeaderWithoutInspectingToken() throws Exception {
        MockHttpServletRequest inboundRequest = new MockHttpServletRequest();
        inboundRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer user-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(inboundRequest));

        HttpHeaders headers = new HttpHeaders();
        HttpRequest outboundRequest = mock(HttpRequest.class);
        when(outboundRequest.getHeaders()).thenReturn(headers);
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(execution.execute(eq(outboundRequest), any(byte[].class))).thenReturn(response);

        ClientHttpResponse result = interceptor.intercept(
                outboundRequest,
                new byte[0],
                execution
        );

        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer user-token");
        assertThat(result).isSameAs(response);
    }

    @Test
    void executesRequestWithoutAuthorizationWhenInboundHeaderIsMissing() throws Exception {
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest())
        );

        HttpHeaders headers = new HttpHeaders();
        HttpRequest outboundRequest = mock(HttpRequest.class);
        when(outboundRequest.getHeaders()).thenReturn(headers);
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(execution.execute(eq(outboundRequest), any(byte[].class))).thenReturn(response);

        ClientHttpResponse result = interceptor.intercept(
                outboundRequest,
                new byte[0],
                execution
        );

        assertThat(headers.containsKey(HttpHeaders.AUTHORIZATION)).isFalse();
        assertThat(result).isSameAs(response);
    }
}
