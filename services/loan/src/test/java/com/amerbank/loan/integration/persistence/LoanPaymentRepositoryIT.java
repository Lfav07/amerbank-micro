package com.amerbank.loan.integration.persistence;

import com.amerbank.loan.model.Loan;
import com.amerbank.loan.model.LoanPayment;
import com.amerbank.loan.model.LoanPaymentStatus;
import com.amerbank.loan.model.LoanStatus;
import com.amerbank.loan.model.LoanType;
import com.amerbank.loan.repository.LoanPaymentRepository;
import com.amerbank.loan.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = "spring.cloud.config.enabled=false")
@ActiveProfiles("test")
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class LoanPaymentRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private LoanPaymentRepository loanPaymentRepository;

    @Autowired
    private LoanRepository loanRepository;

    @BeforeEach
    void setUp() {
        loanPaymentRepository.deleteAllInBatch();
        loanRepository.deleteAllInBatch();
    }

    private Loan saveTestLoan(String loanNumber, Long customerId) {
        Loan loan = Loan.builder()
                .loanNumber(loanNumber)
                .customerId(customerId)
                .accountNumber("ACC0000000001")
                .principalAmount(BigDecimal.valueOf(50000.00))
                .interestRate(BigDecimal.valueOf(5.5))
                .termMonths(60)
                .monthlyPayment(BigDecimal.valueOf(955.06))
                .totalAmount(BigDecimal.valueOf(57303.60))
                .remainingBalance(BigDecimal.valueOf(57303.60))
                .type(LoanType.PERSONAL)
                .status(LoanStatus.DISBURSED)
                .build();
        return loanRepository.save(loan);
    }

    private LoanPayment buildPayment(UUID loanId, int paymentNumber, LoanPaymentStatus status) {
        return LoanPayment.builder()
                .loanId(loanId)
                .paymentNumber(paymentNumber)
                .amountDue(BigDecimal.valueOf(955.06))
                .principalPortion(BigDecimal.valueOf(734.23))
                .interestPortion(BigDecimal.valueOf(220.83))
                .dueDate(LocalDate.now().plusMonths(paymentNumber))
                .status(status)
                .build();
    }

    @Nested
    @DisplayName("Lookup Operations")
    class LookupTests {

        @Test
        @DisplayName("Should find payments by loan ID ordered by payment number")
        void shouldFindPaymentsByLoanIdOrdered() {
            Loan loan = saveTestLoan("LN-0000000001", 1L);
            loanPaymentRepository.save(buildPayment(loan.getId(), 3, LoanPaymentStatus.PENDING));
            loanPaymentRepository.save(buildPayment(loan.getId(), 1, LoanPaymentStatus.PENDING));
            loanPaymentRepository.save(buildPayment(loan.getId(), 2, LoanPaymentStatus.PENDING));

            List<LoanPayment> found = loanPaymentRepository.findAllByLoanIdOrderByPaymentNumberAsc(loan.getId());

            assertEquals(3, found.size());
            assertEquals(1, found.get(0).getPaymentNumber());
            assertEquals(2, found.get(1).getPaymentNumber());
            assertEquals(3, found.get(2).getPaymentNumber());
        }

        @Test
        @DisplayName("Should return empty list when no payments found")
        void shouldReturnEmptyListWhenNoPaymentsFound() {
            List<LoanPayment> found = loanPaymentRepository.findAllByLoanIdOrderByPaymentNumberAsc(UUID.randomUUID());

            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("Should find payment by loan ID and payment number")
        void shouldFindPaymentByLoanIdAndPaymentNumber() {
            Loan loan = saveTestLoan("LN-0000000001", 1L);
            loanPaymentRepository.save(buildPayment(loan.getId(), 1, LoanPaymentStatus.PENDING));

            Optional<LoanPayment> found = loanPaymentRepository.findByLoanIdAndPaymentNumber(loan.getId(), 1);

            assertTrue(found.isPresent());
            assertEquals(1, found.get().getPaymentNumber());
        }

        @Test
        @DisplayName("Should return empty when payment not found by loan ID and payment number")
        void shouldReturnEmptyWhenPaymentNotFound() {
            Optional<LoanPayment> found = loanPaymentRepository.findByLoanIdAndPaymentNumber(
                    UUID.randomUUID(), 1);

            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("Should find payments by loan ID and status")
        void shouldFindPaymentsByLoanIdAndStatus() {
            Loan loan = saveTestLoan("LN-0000000001", 1L);
            loanPaymentRepository.save(buildPayment(loan.getId(), 1, LoanPaymentStatus.PENDING));
            loanPaymentRepository.save(buildPayment(loan.getId(), 2, LoanPaymentStatus.PAID));
            loanPaymentRepository.save(buildPayment(loan.getId(), 3, LoanPaymentStatus.PENDING));

            List<LoanPayment> found = loanPaymentRepository.findAllByLoanIdAndStatus(
                    loan.getId(), LoanPaymentStatus.PENDING);

            assertEquals(2, found.size());
        }

        @Test
        @DisplayName("Should return empty list when no payments found by status")
        void shouldReturnEmptyListWhenNoPaymentsByStatus() {
            Loan loan = saveTestLoan("LN-0000000001", 1L);

            List<LoanPayment> found = loanPaymentRepository.findAllByLoanIdAndStatus(
                    loan.getId(), LoanPaymentStatus.OVERDUE);

            assertTrue(found.isEmpty());
        }
    }

    @Nested
    @DisplayName("Create Operations")
    class CreateTests {

        @Test
        @DisplayName("Should save payment to database")
        void shouldSavePayment() {
            Loan loan = saveTestLoan("LN-0000000001", 1L);
            LoanPayment payment = buildPayment(loan.getId(), 1, LoanPaymentStatus.PENDING);

            LoanPayment saved = loanPaymentRepository.save(payment);

            assertNotNull(saved.getId());
            assertEquals(1, loanPaymentRepository.count());
        }

        @Test
        @DisplayName("Should save multiple payments for same loan")
        void shouldSaveMultiplePaymentsForSameLoan() {
            Loan loan = saveTestLoan("LN-0000000001", 1L);
            loanPaymentRepository.save(buildPayment(loan.getId(), 1, LoanPaymentStatus.PENDING));
            loanPaymentRepository.save(buildPayment(loan.getId(), 2, LoanPaymentStatus.PENDING));
            loanPaymentRepository.save(buildPayment(loan.getId(), 3, LoanPaymentStatus.PENDING));

            assertEquals(3, loanPaymentRepository.count());
        }
    }

    @Nested
    @DisplayName("Pessimistic Lock Tests")
    class PessimisticLockTests {

        @Test
        @DisplayName("Should find payment by loan ID and payment number for update")
        void shouldFindPaymentForUpdate() {
            Loan loan = saveTestLoan("LN-0000000001", 1L);
            LoanPayment payment = loanPaymentRepository.save(buildPayment(loan.getId(), 1, LoanPaymentStatus.PENDING));

            Optional<LoanPayment> found = loanPaymentRepository.findByLoanIdAndPaymentNumberForUpdate(
                    loan.getId(), 1);

            assertTrue(found.isPresent());
            assertEquals(payment.getId(), found.get().getId());
        }

        @Test
        @DisplayName("Should return empty when payment not found for update")
        void shouldReturnEmptyWhenPaymentNotFoundForUpdate() {
            Optional<LoanPayment> found = loanPaymentRepository.findByLoanIdAndPaymentNumberForUpdate(
                    UUID.randomUUID(), 1);

            assertTrue(found.isEmpty());
        }
    }
}
