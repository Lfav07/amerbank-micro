package com.amerbank.transaction.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "transaction")
@Validated
@Getter
@Setter
public class TransactionProperties {

    @NotBlank
    private String accountServiceBase;

    @NotBlank
    private String endpointOwned;

    @NotBlank
    private String endpointDeposit;

    @NotBlank
    private String endpointPayment;

    @NotBlank
    private String endpointRefund;
}