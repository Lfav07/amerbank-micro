package com.amerbank.account.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "jwt")
@Component
@Getter
@Setter
public class JwtProperties {
    @NotBlank private String secret;

    @Min(60000)
    @NotNull private Long expirationMs;

    @Min(10000)
    @NotNull private  Long serviceExpirationMs;

}
