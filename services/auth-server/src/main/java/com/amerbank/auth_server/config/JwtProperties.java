package com.amerbank.auth_server.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
@Validated
public class JwtProperties {
    @NotBlank
    private String secret;

    @Min(60000)
    private long expirationMs;

    @Min(10000)
    private long serviceTokenExpirationMs;
}
