package com.amerbank.account.dto;

import java.math.BigDecimal;

public record ServiceRefundBalanceRequest(
        Long customerId,
        String fromAccountNumber,
        String toAccountNumber,
        BigDecimal amount
) {
}

