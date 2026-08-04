package com.amerbank.loan.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "Loan repayment request")
public record LoanRepaymentRequest(
        @Schema(description = "Loan number", example = "LN-1234567890")
        @NotBlank
        String loanNumber,

        @Schema(description = "Repayment amount", example = "500.00")
        @NotNull
        @DecimalMin(value = "0.01", message = "Repayment amount must be positive")
        BigDecimal amount
) {}