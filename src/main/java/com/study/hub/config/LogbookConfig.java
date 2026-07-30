package com.study.hub.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.BodyFilter;
import org.zalando.logbook.Sink;
import org.zalando.logbook.core.BodyFilters;
import org.zalando.logbook.json.JsonBodyFilters;
import org.zalando.logbook.json.JsonHttpLogFormatter;
import org.zalando.logbook.logstash.LogstashLogbackSink;

import static org.zalando.logbook.BodyFilter.merge;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LogbookProperties.class)
public class LogbookConfig {

    private static final String MASK = "***";

    @Bean
    @ConditionalOnProperty(prefix = "log-config", name = "json-logging", havingValue = "true")
    @ConditionalOnMissingBean(Sink.class)
    public Sink logbookSink() {
        return new LogstashLogbackSink(new JsonHttpLogFormatter());
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "log-config",
            name = "mask-sensitive-data",
            havingValue = "true",
            matchIfMissing = true
    )
    @ConditionalOnMissingBean(BodyFilter.class)
    public BodyFilter logbookBodyFilter(LogbookProperties properties) {
        return merge(
                BodyFilters.defaultValue(),
                JsonBodyFilters.replaceJsonStringProperty(
                        properties.sensitiveFields(),
                        MASK
                )
        );
    }
}
