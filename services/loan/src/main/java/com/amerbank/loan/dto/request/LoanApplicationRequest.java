package com.amerbank.loan.dto.request;

import com.amerbank.loan.model.LoanType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "Loan application request")
public record LoanApplicationRequest(
        @Schema(description = "Type of loan to apply for", example = "PERSONAL")
        @NotNull
        LoanType type,

        @Schema(description = "Loan amount requested", example = "50000.00")
        @NotNull
        @DecimalMin(value = "100.00", message = "Minimum loan amount is 100.00")
        BigDecimal principalAmount,

        @Schema(description = "Annual interest rate", example = "5.5")
        @NotNull
        @DecimalMin(value = "0.1", message = "Interest rate must be positive")
        BigDecimal interestRate,

        @Schema(description = "Loan term in months", example = "60")
        @NotNull
        @Min(value = 1, message = "Term must be at least 1 month")
        Integer termMonths,

        @Schema(description = "Target account number for disbursement", example = "550e8400e29b")
        @NotBlank
        String accountNumber
) {}