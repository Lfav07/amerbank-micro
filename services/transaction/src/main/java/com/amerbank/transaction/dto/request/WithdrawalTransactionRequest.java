package com.amerbank.transaction.dto.request;

import java.math.BigDecimal;

public record WithdrawalTransactionRequest(

        BigDecimal amount,
        String description,
        String fromAccountNumber
) {
}
