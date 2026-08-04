package com.amerbank.loan.repository;

import com.amerbank.loan.model.Loan;
import com.amerbank.loan.model.LoanStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanRepository extends JpaRepository<Loan, UUID> {

    Optional<Loan> findByLoanNumber(String loanNumber);

    List<Loan> findAllByCustomerId(Long customerId);

    List<Loan> findAllByStatus(LoanStatus status);

    boolean existsByLoanNumber(String loanNumber);

    boolean existsByCustomerIdAndStatusIn(Long customerId, List<LoanStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Loan l WHERE l.loanNumber = :loanNumber")
    Optional<Loan> findByLoanNumberForUpdate(@Param("loanNumber") String loanNumber);

    @Query("SELECT l FROM Loan l WHERE l.customerId = :customerId AND l.status IN :statuses")
    List<Loan> findActiveLoansByCustomerId(@Param("customerId") Long customerId, @Param("statuses") List<LoanStatus> statuses);
}