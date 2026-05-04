package com.amerbank.account.dto.request;

import java.math.BigDecimal;

public record ServiceRefundBalanceRequest(
        Long customerId,
        String fromAccountNumber,
        String toAccountNumber,
        BigDecimal amount
) {
}

