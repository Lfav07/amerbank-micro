package com.amerbank.transaction.dto;

import java.math.BigDecimal;

public record WithdrawalTransactionRequest(

        BigDecimal amount,
        String description,
        String fromAccountNumber
) {
}
