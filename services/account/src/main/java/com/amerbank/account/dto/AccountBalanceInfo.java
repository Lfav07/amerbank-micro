package com.amerbank.account.dto;

import com.amerbank.account.model.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Account balance information")
public record AccountBalanceInfo(
        @Schema(description = "Account number", example = "550e8400-e29b-41d4-a716-446655440000")
        String accountNumber,
        @Schema(description = "Account type", example = "CHECKING")
        AccountType type,
        @Schema(description = "Account balance", example = "1000.00")
        BigDecimal balance
) {}
