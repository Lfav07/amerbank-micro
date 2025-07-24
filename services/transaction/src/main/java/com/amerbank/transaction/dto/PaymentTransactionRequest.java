package com.amerbank.transaction.dto;

import java.math.BigDecimal;

public record PaymentTransactionRequest(
        BigDecimal amount,
        String description,
        String fromAccountNumber,
        String toAccountNumber
) {
}
