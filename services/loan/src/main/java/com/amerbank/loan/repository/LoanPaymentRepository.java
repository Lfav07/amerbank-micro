package com.amerbank.loan.repository;

import com.amerbank.loan.model.LoanPayment;
import com.amerbank.loan.model.LoanPaymentStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanPaymentRepository extends JpaRepository<LoanPayment, UUID> {

    List<LoanPayment> findAllByLoanIdOrderByPaymentNumberAsc(UUID loanId);

    Optional<LoanPayment> findByLoanIdAndPaymentNumber(UUID loanId, Integer paymentNumber);

    List<LoanPayment> findAllByLoanIdAndStatus(UUID loanId, LoanPaymentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT lp FROM LoanPayment lp WHERE lp.loanId = :loanId AND lp.paymentNumber = :paymentNumber")
    Optional<LoanPayment> findByLoanIdAndPaymentNumberForUpdate(
            @Param("loanId") UUID loanId,
            @Param("paymentNumber") Integer paymentNumber
    );
}