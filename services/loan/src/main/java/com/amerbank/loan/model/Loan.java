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
@Table(name = "loans", schema = "loan")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @NotNull
    @Column(name = "loan_number", unique = true, nullable = false)
    private String loanNumber;

    @NotNull
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @NotNull
    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "principal_amount", nullable = false)
    private BigDecimal principalAmount;

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "interest_rate", nullable = false)
    private BigDecimal interestRate;

    @NotNull
    @Column(name = "term_months", nullable = false)
    private Integer termMonths;

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "monthly_payment", nullable = false)
    private BigDecimal monthlyPayment;

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "remaining_balance", nullable = false)
    private BigDecimal remainingBalance;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanType type;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "disbursed_at")
    private LocalDateTime disbursedAt;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}