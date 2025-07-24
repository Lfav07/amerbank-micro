package com.amerbank.transaction.dto;

import java.math.BigDecimal;

public record TransferTransactionRequest(

        BigDecimal amount,
        String description,
        String fromAccountNumber,
        String toAccountNumber
) {
}
