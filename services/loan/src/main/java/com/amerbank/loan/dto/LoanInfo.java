package com.amerbank.loan.dto;

import com.amerbank.loan.model.LoanStatus;
import com.amerbank.loan.model.LoanType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Loan summary info for list views")
public record LoanInfo(
        @Schema(description = "Loan unique identifier")
        UUID id,

        @Schema(description = "Loan number", example = "LN-1234567890")
        String loanNumber,

        @Schema(description = "Loan principal amount", example = "50000.00")
        BigDecimal principalAmount,

        @Schema(description = "Remaining balance", example = "45000.00")
        BigDecimal remainingBalance,

        @Schema(description = "Loan type", example = "PERSONAL")
        LoanType type,

        @Schema(description = "Loan status", example = "ACTIVE")
        LoanStatus status,

        @Schema(description = "Monthly payment amount", example = "955.06")
        BigDecimal monthlyPayment
) {}