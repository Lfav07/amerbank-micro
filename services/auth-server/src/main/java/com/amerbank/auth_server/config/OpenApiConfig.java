package com.amerbank.auth_server.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String JWT_SCHEME = "Bearer JWT";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Amerbank Auth Server API")
                        .version("1.0.0")
                        .description("""
                                Authentication and authorization service for Amerbank microservices architecture.
                                
                                Provides user registration, login, and profile management endpoints with JWT-based authentication.
                                
                                ## Authentication
                                All protected endpoints require a JWT token in the Authorization header:
                                ```
                                Authorization: Bearer {token}
                                ```
                                
                                ## Roles
                                - `ROLE_USER`: Regular user access
                                
                                - `ROLE_ADMIN`: Administrative access
                                """)
                        .contact(new Contact()
                                .name("Amerbank Team")
                                .email("dev@amerbank.com")))
                .addSecurityItem(new SecurityRequirement().addList(JWT_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(JWT_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Authorization header using the Bearer scheme. Example: 'Authorization: Bearer {token}'")));
    }
}
