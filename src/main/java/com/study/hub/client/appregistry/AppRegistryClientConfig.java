package com.study.hub.client.appregistry;

import com.study.hub.client.AuthorizationHeaderRelayInterceptor;
import com.study.hub.client.OutboundTraceHeadersInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.net.URI;

@Configuration(proxyBeanMethods = false)
class AppRegistryClientConfig {

    @Bean
    RestClient appRegistryRestClient(
            RestClient.Builder builder,
            @Value("${app.clients.app-registry.base-url}") URI baseUrl,
            OutboundTraceHeadersInterceptor traceHeadersInterceptor,
            AuthorizationHeaderRelayInterceptor authorizationHeaderRelayInterceptor
    ) {
        return builder
                .baseUrl(baseUrl.toString())
                .requestInterceptor(traceHeadersInterceptor)
                .requestInterceptor(authorizationHeaderRelayInterceptor)
                .build();
    }
}
