package com.amerbank.account.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "account")
@Getter
@Setter
public class AccountProperties {

    @NotBlank
    private String prefix;

    @Min(1)
    private int bodyDigits;

    @Min(1)
    private long upperBound;

    @Min(1)
    private int maxAttempts;


}
