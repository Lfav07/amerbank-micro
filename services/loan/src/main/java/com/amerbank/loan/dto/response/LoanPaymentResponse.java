package com.amerbank.loan.dto.response;

import com.amerbank.loan.model.LoanPaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Loan payment schedule response")
public record LoanPaymentResponse(
        @Schema(description = "Payment unique identifier")
        UUID id,

        @Schema(description = "Payment number in schedule", example = "1")
        Integer paymentNumber,

        @Schema(description = "Amount due for this payment", example = "955.06")
        BigDecimal amountDue,

        @Schema(description = "Principal portion of payment", example = "734.23")
        BigDecimal principalPortion,

        @Schema(description = "Interest portion of payment", example = "220.83")
        BigDecimal interestPortion,

        @Schema(description = "Due date for this payment")
        LocalDate dueDate,

        @Schema(description = "Date when payment was made")
        LocalDateTime paidDate,

        @Schema(description = "Payment status", example = "PAID")
        LoanPaymentStatus status
) {}