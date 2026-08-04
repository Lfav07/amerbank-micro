package com.amerbank.loan.config;

import java.math.BigDecimal;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "loan")
public class LoanProperties {
    private String prefix = "LN";
    private int bodyDigits = 10;
    private long upperBound = 10000000000L;
    private int maxAttempts = 5;
    private BigDecimal minAmount = new BigDecimal("100.00");
    private BigDecimal maxAmount = new BigDecimal("1000000.00");
    private int minTermMonths = 1;
    private int maxTermMonths = 360;
    private String accountServiceBase;
    private String customerServiceBase;
    private String endpointOwned;
    private String endpointDeposit;
    private String endpointWithdraw;
    private String endpointPayment;
}