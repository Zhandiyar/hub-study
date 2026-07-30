package com.study.hub.client.appregistry;

import com.study.hub.web.TraceContext;
import org.slf4j.MDC;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.web.client.RequestAttributePrincipalResolver;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AppRegistryClientProperties.class)
class AppRegistryClientConfig {

    @Bean
    RestClient appRegistryRestClient(
            RestClient.Builder builder,
            AppRegistryClientProperties properties,
            OAuth2AuthorizedClientManager authorizedClientManager,
            OAuth2AuthorizedClientService authorizedClientService
    ) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());

        var oauth2Interceptor = new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
        oauth2Interceptor.setPrincipalResolver(new RequestAttributePrincipalResolver());
        oauth2Interceptor.setAuthorizationFailureHandler(
                OAuth2ClientHttpRequestInterceptor.authorizationFailureHandler(
                        authorizedClientService
                )
        );

        return builder
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    copyMdcHeader(request, TraceContext.TRACE_ID_HEADER, TraceContext.TRACE_ID_MDC_KEY);
                    copyMdcHeader(
                            request,
                            TraceContext.CORRELATION_ID_HEADER,
                            TraceContext.CORRELATION_ID_MDC_KEY
                    );
                    return execution.execute(request, body);
                })
                .requestInterceptor(oauth2Interceptor)
                .build();
    }

    private void copyMdcHeader(
            HttpRequest request,
            String headerName,
            String mdcKey
    ) {
        String value = MDC.get(mdcKey);
        if (value != null && !value.isBlank()) {
            request.getHeaders().set(headerName, value);
        }
    }
}
