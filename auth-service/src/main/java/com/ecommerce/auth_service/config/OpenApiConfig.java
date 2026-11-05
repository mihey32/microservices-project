package com.ecommerce.auth_service.config;

import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_KEY = "bearerAuth";

    @Bean
    public OpenAPI authServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Auth Service API")
                        .version("1.0.0")
                        .description("""
                                Аутентификация и управление аккаунтами.
                                Защищённые эндпоинты требуют заголовок `Authorization: Bearer <accessToken>`.
                                """))
                .components(new Components().addSecuritySchemes(BEARER_KEY,
                        new SecurityScheme()
                                .name(BEARER_KEY)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

}
