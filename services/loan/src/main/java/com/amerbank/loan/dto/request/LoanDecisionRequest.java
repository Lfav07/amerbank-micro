package com.amerbank.loan.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Admin loan decision request")
public record LoanDecisionRequest(
        @Schema(description = "Loan number", example = "LN-1234567890")
        @NotBlank
        String loanNumber,

        @Schema(description = "Rejection reason (required if rejecting)", example = "Insufficient credit score")
        String rejectionReason
) {}