package com.amerbank.loan.integration.persistence;

import com.amerbank.loan.model.Loan;
import com.amerbank.loan.model.LoanStatus;
import com.amerbank.loan.model.LoanType;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = "spring.cloud.config.enabled=false")
@ActiveProfiles("test")
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class LoanRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private LoanRepository loanRepository;

    @BeforeEach
    void setUp() {
        loanRepository.deleteAllInBatch();
    }

    private Loan buildLoan(String loanNumber, Long customerId, LoanStatus status) {
        return Loan.builder()
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
                .status(status)
                .build();
    }

    @Nested
    @DisplayName("Lookup Operations")
    class LookupTests {

        @Test
        @DisplayName("Should find loan by loan number")
        void shouldFindLoanByLoanNumber() {
            Loan loan = buildLoan("LN-0000000001", 1L, LoanStatus.ACTIVE);
            loanRepository.save(loan);

            Optional<Loan> found = loanRepository.findByLoanNumber("LN-0000000001");

            assertTrue(found.isPresent());
            assertEquals("LN-0000000001", found.get().getLoanNumber());
        }

        @Test
        @DisplayName("Should return empty when loan number not found")
        void shouldReturnEmptyWhenLoanNumberNotFound() {
            Optional<Loan> found = loanRepository.findByLoanNumber("LN-9999999999");

            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("Should find loans by customer ID")
        void shouldFindLoansByCustomerId() {
            loanRepository.save(buildLoan("LN-0000000001", 1L, LoanStatus.ACTIVE));
            loanRepository.save(buildLoan("LN-0000000002", 1L, LoanStatus.PENDING));
            loanRepository.save(buildLoan("LN-0000000003", 2L, LoanStatus.ACTIVE));

            List<Loan> found = loanRepository.findAllByCustomerId(1L);

            assertEquals(2, found.size());
        }

        @Test
        @DisplayName("Should return empty list when no loans found by customer ID")
        void shouldReturnEmptyListWhenNoLoansFoundByCustomerId() {
            List<Loan> found = loanRepository.findAllByCustomerId(999L);

            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("Should find loans by status")
        void shouldFindLoansByStatus() {
            loanRepository.save(buildLoan("LN-0000000001", 1L, LoanStatus.ACTIVE));
            loanRepository.save(buildLoan("LN-0000000002", 2L, LoanStatus.ACTIVE));
            loanRepository.save(buildLoan("LN-0000000003", 3L, LoanStatus.PENDING));

            List<Loan> found = loanRepository.findAllByStatus(LoanStatus.ACTIVE);

            assertEquals(2, found.size());
        }

        @Test
        @DisplayName("Should find active loans by customer ID with status filter")
        void shouldFindActiveLoansByCustomerId() {
            loanRepository.save(buildLoan("LN-0000000001", 1L, LoanStatus.ACTIVE));
            loanRepository.save(buildLoan("LN-0000000002", 1L, LoanStatus.DISBURSED));
            loanRepository.save(buildLoan("LN-0000000003", 1L, LoanStatus.PENDING));
            loanRepository.save(buildLoan("LN-0000000004", 2L, LoanStatus.ACTIVE));

            List<Loan> found = loanRepository.findActiveLoansByCustomerId(1L,
                    List.of(LoanStatus.ACTIVE, LoanStatus.DISBURSED));

            assertEquals(2, found.size());
        }
    }

    @Nested
    @DisplayName("Existence Checking Operations")
    class ExistenceTests {

        @Test
        @DisplayName("Should return true when loan exists by loan number")
        void shouldReturnTrueWhenLoanExistsByLoanNumber() {
            loanRepository.save(buildLoan("LN-0000000001", 1L, LoanStatus.ACTIVE));

            boolean exists = loanRepository.existsByLoanNumber("LN-0000000001");

            assertTrue(exists);
        }

        @Test
        @DisplayName("Should return false when loan does not exist by loan number")
        void shouldReturnFalseWhenLoanDoesNotExistByLoanNumber() {
            boolean exists = loanRepository.existsByLoanNumber("LN-9999999999");

            assertFalse(exists);
        }

        @Test
        @DisplayName("Should return true when customer has active or pending loan")
        void shouldReturnTrueWhenCustomerHasActiveLoan() {
            loanRepository.save(buildLoan("LN-0000000001", 1L, LoanStatus.ACTIVE));

            boolean exists = loanRepository.existsByCustomerIdAndStatusIn(1L,
                    List.of(LoanStatus.PENDING, LoanStatus.APPROVED, LoanStatus.DISBURSED, LoanStatus.ACTIVE));

            assertTrue(exists);
        }

        @Test
        @DisplayName("Should return false when customer has no active loans")
        void shouldReturnFalseWhenCustomerHasNoActiveLoans() {
            loanRepository.save(buildLoan("LN-0000000001", 1L, LoanStatus.PAID_OFF));

            boolean exists = loanRepository.existsByCustomerIdAndStatusIn(1L,
                    List.of(LoanStatus.PENDING, LoanStatus.APPROVED, LoanStatus.DISBURSED, LoanStatus.ACTIVE));

            assertFalse(exists);
        }
    }

    @Nested
    @DisplayName("Create Operations")
    class CreateTests {

        @Test
        @DisplayName("Should save loan to database")
        void shouldSaveLoan() {
            Loan loan = buildLoan("LN-0000000001", 1L, LoanStatus.PENDING);

            Loan saved = loanRepository.save(loan);

            assertNotNull(saved.getId());
            assertEquals(1, loanRepository.count());
        }

        @Test
        @DisplayName("Should save multiple loans for same customer")
        void shouldSaveMultipleLoansForSameCustomer() {
            loanRepository.save(buildLoan("LN-0000000001", 1L, LoanStatus.ACTIVE));
            loanRepository.save(buildLoan("LN-0000000002", 1L, LoanStatus.PAID_OFF));

            assertEquals(2, loanRepository.count());
        }
    }

    @Nested
    @DisplayName("Pessimistic Lock Tests")
    class PessimisticLockTests {

        @Test
        @DisplayName("Should find loan by loan number for update")
        void shouldFindLoanByLoanNumberForUpdate() {
            Loan loan = loanRepository.save(buildLoan("LN-0000000001", 1L, LoanStatus.ACTIVE));

            Optional<Loan> found = loanRepository.findByLoanNumberForUpdate("LN-0000000001");

            assertTrue(found.isPresent());
            assertEquals(loan.getLoanNumber(), found.get().getLoanNumber());
        }

        @Test
        @DisplayName("Should return empty when loan not found for update")
        void shouldReturnEmptyWhenLoanNotFoundForUpdate() {
            Optional<Loan> found = loanRepository.findByLoanNumberForUpdate("LN-9999999999");

            assertTrue(found.isEmpty());
        }
    }
}
