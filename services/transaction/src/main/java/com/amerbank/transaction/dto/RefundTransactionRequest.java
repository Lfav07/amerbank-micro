package com.amerbank.transaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Refund transaction request")
public record RefundTransactionRequest(
        @Schema(description = "ID of the transaction to refund", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull UUID transactionId
) {
}
