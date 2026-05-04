package com.amerbank.transaction.dto.request;

import java.math.BigDecimal;

public record ServiceDepositBalanceRequest(
        Long customerId,
        String accountNumber,
        BigDecimal amount
)


{}
