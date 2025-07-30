package com.amerbank.transaction.dto;

import java.util.UUID;
import java.math.BigDecimal;

public record RefundTransactionRequest(
        UUID transactionId
) {
}
