package com.amerbank.account.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@ConfigurationProperties(prefix = "account")
@Getter
@Setter
@Component
public class AccountProperties {

    //Account number generation
    @NotBlank
    private String prefix;

    @Min(1)
    private int bodyDigits;

    @Min(1)
    private long upperBound;

    @Min(1)
    private int maxAttempts;

    public BigDecimal getInitialBalance() {
        return initialBalance.setScale(2, RoundingMode.UNNECESSARY);
    }

    //Account registration
    private BigDecimal initialBalance;


}
