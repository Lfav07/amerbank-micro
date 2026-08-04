package com.amerbank.loan.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "Internal loan repayment request")
public record ServiceRepaymentRequest(
        @Schema(description = "Customer ID")
        @NotNull
        Long customerId,

        @Schema(description = "Loan number", example = "LN-1234567890")
        @NotBlank
        String loanNumber,

        @Schema(description = "Repayment amount", example = "500.00")
        @NotNull
        BigDecimal amount
) {}