package com.amerbank.transaction.dto;

import jakarta.persistence.Column;

import java.math.BigDecimal;

public record DepositTransactionRequest(
        BigDecimal amount,
        String description,
        String fromAccountNumber,
        String toAccountNumber



) {
}
