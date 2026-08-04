package com.amerbank.loan.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "Internal loan disbursement request")
public record ServiceDisbursementRequest(
        @Schema(description = "Customer ID")
        @NotNull
        Long customerId,

        @Schema(description = "Loan number", example = "LN-1234567890")
        @NotBlank
        String loanNumber,

        @Schema(description = "Disbursement amount", example = "50000.00")
        @NotNull
        BigDecimal amount,

        @Schema(description = "Target account number for disbursement")
        @NotBlank
        String accountNumber
) {}