package com.study.hub.client.partner;

import com.study.hub.client.AuthorizationHeaderRelayInterceptor;
import com.study.hub.client.OutboundTraceHeadersInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor;

@Configuration(proxyBeanMethods = false)
class PartnerApiClientConfig {

    @Bean
    RestClient partnerApiRestClient(
            RestClient.Builder builder,
            OutboundTraceHeadersInterceptor traceHeadersInterceptor,
            AuthorizationHeaderRelayInterceptor authorizationHeaderRelayInterceptor,
            LogbookClientHttpRequestInterceptor logbookInterceptor
    ) {
        return builder
                .requestInterceptor(traceHeadersInterceptor)
                .requestInterceptor(authorizationHeaderRelayInterceptor)
                .requestInterceptor(logbookInterceptor)
                .build();
    }
}
