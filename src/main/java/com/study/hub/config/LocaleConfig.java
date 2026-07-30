package com.study.hub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

@Configuration(proxyBeanMethods = false)
public class LocaleConfig {

    public static final Locale RUSSIAN = Locale.forLanguageTag("ru");
    public static final Locale KAZAKH = Locale.forLanguageTag("kk");
    public static final Locale ENGLISH = Locale.ENGLISH;

    @Bean
    LocaleResolver localeResolver() {
        var resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(List.of(RUSSIAN, KAZAKH, ENGLISH));
        resolver.setDefaultLocale(RUSSIAN);
        return resolver;
    }
}
