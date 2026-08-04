package com.amerbank.loan.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "loan_payments", schema = "loan")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoanPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @NotNull
    @Column(name = "loan_id", nullable = false)
    private UUID loanId;

    @NotNull
    @Column(name = "payment_number", nullable = false)
    private Integer paymentNumber;

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "amount_due", nullable = false)
    private BigDecimal amountDue;

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "principal_portion", nullable = false)
    private BigDecimal principalPortion;

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "interest_portion", nullable = false)
    private BigDecimal interestPortion;

    @NotNull
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_date")
    private LocalDateTime paidDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanPaymentStatus status;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}