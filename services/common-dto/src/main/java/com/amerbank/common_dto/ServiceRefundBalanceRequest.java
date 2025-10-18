package com.amerbank.common_dto;

import java.math.BigDecimal;

public record ServiceRefundBalanceRequest(
        Long customerId,
        String fromAccountNumber,
        String toAccountNumber,
        BigDecimal amount
) {
}

