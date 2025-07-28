package com.amerbank.transaction.dto;

import com.amerbank.transaction.model.TransactionStatus;
import com.amerbank.transaction.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        BigDecimal amount,
        String fromAccountNumber,
        String toAccountNumber,
        TransactionType type,
        TransactionStatus status,
        LocalDateTime createdAt


) {
}
