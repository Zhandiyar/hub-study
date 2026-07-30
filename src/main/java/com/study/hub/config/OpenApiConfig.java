package com.study.hub.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("!prod")
public class OpenApiConfig {

    private static final String BEARER_JWT = "bearer-jwt";

    @Bean
    public OpenAPI hubOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Hub партнёрских приложений")
                        .description("Единая точка доступа к партнёрским приложениям")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_JWT, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_JWT));
    }
}
