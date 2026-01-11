package com.amerbank.transaction.dto;

import java.math.BigDecimal;

public record ServiceDepositBalanceRequest(
        Long customerId,
        String accountNumber,
        BigDecimal amount
)


{}
