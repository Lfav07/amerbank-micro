package com.amerbank.transaction.dto.request;

import java.math.BigDecimal;

public record ServiceRefundBalanceRequest(
        Long customerId,
        String fromAccountNumber,
        String toAccountNumber,
        BigDecimal amount
) {
}

