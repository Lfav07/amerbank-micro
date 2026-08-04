package com.amerbank.loan.config;

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
                        .title("Amerbank Loan Service API")
                        .version("1.0.0")
                        .description("""
                                Loan management service for Amerbank microservices architecture.
                                
                                Handles loan application, approval, disbursement, repayment, and closure.
                                
                                ## Authentication
                                All protected endpoints require a JWT token in the Authorization header:
                                ```
                                Authorization: Bearer {token}
                                ```
                                
                                ## Roles
                                - `ROLE_USER`: Regular customer access
                                - `ROLE_ADMIN`: Administrative access
                                - `SCOPE_service`: Internal service-to-service access
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