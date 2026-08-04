package com.amerbank.loan.dto.response;

import com.amerbank.loan.model.LoanStatus;
import com.amerbank.loan.model.LoanType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Loan response")
public record LoanResponse(
        @Schema(description = "Loan unique identifier")
        UUID id,

        @Schema(description = "Loan number", example = "LN-1234567890")
        String loanNumber,

        @Schema(description = "Customer ID", example = "1")
        Long customerId,

        @Schema(description = "Account number for disbursement", example = "550e8400e29b")
        String accountNumber,

        @Schema(description = "Loan principal amount", example = "50000.00")
        BigDecimal principalAmount,

        @Schema(description = "Annual interest rate", example = "5.5")
        BigDecimal interestRate,

        @Schema(description = "Loan term in months", example = "60")
        Integer termMonths,

        @Schema(description = "Monthly payment amount", example = "955.06")
        BigDecimal monthlyPayment,

        @Schema(description = "Total loan amount with interest", example = "57303.60")
        BigDecimal totalAmount,

        @Schema(description = "Remaining balance", example = "45000.00")
        BigDecimal remainingBalance,

        @Schema(description = "Loan type", example = "PERSONAL")
        LoanType type,

        @Schema(description = "Loan status", example = "ACTIVE")
        LoanStatus status,

        @Schema(description = "Date when loan was disbursed")
        LocalDateTime disbursedAt,

        @Schema(description = "Loan maturity date")
        LocalDate maturityDate
) {}