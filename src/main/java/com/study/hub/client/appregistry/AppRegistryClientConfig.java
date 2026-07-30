package com.study.hub.client.appregistry;

import com.study.hub.web.TraceContext;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.web.client.RequestAttributePrincipalResolver;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AppRegistryClientProperties.class)
class AppRegistryClientConfig {

    @Bean(destroyMethod = "close")
    CloseableHttpClient appRegistryHttpClient(AppRegistryClientProperties properties) {
        var connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.of(properties.connectTimeout()))
                .setSocketTimeout(Timeout.of(properties.readTimeout()))
                .build();

        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connectionConfig)
                .setMaxConnTotal(properties.maxConnections())
                .setMaxConnPerRoute(properties.maxConnectionsPerRoute())
                .build();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .disableAutomaticRetries()
                .disableCookieManagement()
                .disableRedirectHandling()
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.of(properties.idleConnectionTimeout()))
                .build();
    }

    @Bean
    RestClient appRegistryRestClient(
            RestClient.Builder builder,
            AppRegistryClientProperties properties,
            @Qualifier("appRegistryHttpClient") CloseableHttpClient httpClient,
            OAuth2AuthorizedClientManager authorizedClientManager,
            OAuth2AuthorizedClientService authorizedClientService
    ) {
        var requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        requestFactory.setConnectionRequestTimeout(properties.connectionRequestTimeout());
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
