package com.amerbank.transaction.dto;

import com.amerbank.transaction.model.TransactionStatus;
import com.amerbank.transaction.model.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Transaction response")
public record TransactionResponse(
        @Schema(description = "Transaction unique identifier")
        UUID id,
        @Schema(description = "Transaction amount", example = "100.00")
        BigDecimal amount,
        @Schema(description = "Source account number", example = "550e8400-e29b-41d4-a716-446655440000")
        String fromAccountNumber,
        @Schema(description = "Destination account number", example = "550e8400-e29b-41d4-a716-446655440001")
        String toAccountNumber,
        @Schema(description = "Transaction type", example = "DEPOSIT")
        TransactionType type,
        @Schema(description = "Transaction status", example = "COMPLETED")
        TransactionStatus status,
        @Schema(description = "Transaction creation timestamp")
        LocalDateTime createdAt


) {
}
