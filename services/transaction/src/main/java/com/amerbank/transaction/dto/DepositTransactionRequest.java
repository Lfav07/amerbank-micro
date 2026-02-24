package com.amerbank.transaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Deposit transaction request")
public record DepositTransactionRequest(
        @Schema(description = "Deposit amount", example = "100.00")
        @NotNull @Positive BigDecimal amount,
        @Schema(description = "Transaction description", example = "Salary deposit")
        String description,
        @Schema(description = "Source account number", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotBlank String fromAccountNumber,
        @Schema(description = "Destination account number", example = "550e8400-e29b-41d4-a716-446655440001")
        @NotBlank String toAccountNumber


) {
}
