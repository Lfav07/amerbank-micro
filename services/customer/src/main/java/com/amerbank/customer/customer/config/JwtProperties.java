package com.amerbank.customer.customer.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    @NotBlank
    private String secret;

    @NotNull
    private  Long serviceTokenExpirationMs;
}
