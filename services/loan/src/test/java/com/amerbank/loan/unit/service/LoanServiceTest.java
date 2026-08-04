package com.amerbank.loan.unit.service;

import com.amerbank.loan.client.AccountServiceClientInterface;
import com.amerbank.loan.config.LoanProperties;
import com.amerbank.loan.dto.LoanInfo;
import com.amerbank.loan.dto.request.LoanApplicationRequest;
import com.amerbank.loan.dto.request.LoanRepaymentRequest;
import com.amerbank.loan.dto.response.LoanPaymentResponse;
import com.amerbank.loan.dto.response.LoanResponse;
import com.amerbank.loan.exception.InsufficientRepaymentAmountException;
import com.amerbank.loan.exception.LoanAlreadyExistsException;
import com.amerbank.loan.exception.LoanNotFoundException;
import com.amerbank.loan.exception.LoanNotDisbursedException;
import com.amerbank.loan.exception.LoanNotEligibleException;
import com.amerbank.loan.exception.LoanOwnershipException;
import com.amerbank.loan.model.Loan;
import com.amerbank.loan.model.LoanPayment;
import com.amerbank.loan.model.LoanPaymentStatus;
import com.amerbank.loan.model.LoanStatus;
import com.amerbank.loan.model.LoanType;
import com.amerbank.loan.repository.LoanPaymentRepository;
import com.amerbank.loan.repository.LoanRepository;
import com.amerbank.loan.service.LoanMapper;
import com.amerbank.loan.service.LoanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LoanPaymentRepository loanPaymentRepository;

    @Mock
    private AccountServiceClientInterface accountServiceClient;


    private LoanMapper loanMapper;

    private LoanProperties loanProperties;

    private LoanService loanService;

    private static final Long CUSTOMER_ID = 1L;
    private static final String ACCOUNT_NUMBER = "ACC0000000001";
    private static final String LOAN_NUMBER = "LN-1234567890";

    @BeforeEach
    void setUp() {
        loanMapper = new LoanMapper();
        loanProperties = new LoanProperties();
        loanProperties.setPrefix("LN");
        loanProperties.setBodyDigits(10);
        loanProperties.setUpperBound(10000000000L);
        loanProperties.setMaxAttempts(5);

        loanService = new LoanService(
                loanRepository,
                loanPaymentRepository,
                loanMapper,
                accountServiceClient,
                loanProperties
        );
    }

    private Loan buildLoan(LoanStatus status) {
        return Loan.builder()
                .id(UUID.randomUUID())
                .loanNumber(LOAN_NUMBER)
                .customerId(CUSTOMER_ID)
                .accountNumber(ACCOUNT_NUMBER)
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

    // ==================== applyForLoan Tests ====================

    @Nested
    @DisplayName("applyForLoan")
    class ApplyForLoanTests {

        @Test
        @DisplayName("Should create loan application successfully")
        void shouldCreateLoanApplication() {
            LoanApplicationRequest request = new LoanApplicationRequest(
                    LoanType.PERSONAL,
                    BigDecimal.valueOf(50000.00),
                    BigDecimal.valueOf(5.5),
                    60,
                    ACCOUNT_NUMBER
            );

            when(accountServiceClient.isAccountOwned(CUSTOMER_ID, ACCOUNT_NUMBER)).thenReturn(true);
            when(loanRepository.existsByCustomerIdAndStatusIn(eq(CUSTOMER_ID), anyList())).thenReturn(false);
            when(loanRepository.existsByLoanNumber(anyString())).thenReturn(false);
            when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
                Loan loan = invocation.getArgument(0);
                loan.setId(UUID.randomUUID());
                return loan;
            });

            LoanResponse response = loanService.applyForLoan(request, CUSTOMER_ID);

            assertNotNull(response);
            assertEquals(CUSTOMER_ID, response.customerId());
            assertEquals(ACCOUNT_NUMBER, response.accountNumber());
            assertEquals(LoanStatus.PENDING, response.status());
            assertEquals(LoanType.PERSONAL, response.type());
            assertNotNull(response.loanNumber());
            assertTrue(response.loanNumber().startsWith("LN-"));

            ArgumentCaptor<Loan> captor = ArgumentCaptor.forClass(Loan.class);
            verify(loanRepository).save(captor.capture());
            Loan saved = captor.getValue();
            assertEquals(BigDecimal.valueOf(50000.00), saved.getPrincipalAmount());
            assertNotNull(saved.getMonthlyPayment());
            assertNotNull(saved.getTotalAmount());
            assertEquals(saved.getTotalAmount(), saved.getRemainingBalance());
        }

        @Test
        @DisplayName("Should throw LoanAlreadyExistsException when customer has active loan")
        void shouldThrowWhenCustomerHasActiveLoan() {
            LoanApplicationRequest request = new LoanApplicationRequest(
                    LoanType.PERSONAL,
                    BigDecimal.valueOf(50000.00),
                    BigDecimal.valueOf(5.5),
                    60,
                    ACCOUNT_NUMBER
            );

            when(accountServiceClient.isAccountOwned(CUSTOMER_ID, ACCOUNT_NUMBER)).thenReturn(true);
            when(loanRepository.existsByCustomerIdAndStatusIn(eq(CUSTOMER_ID), anyList())).thenReturn(true);

            assertThrows(LoanAlreadyExistsException.class, () ->
                    loanService.applyForLoan(request, CUSTOMER_ID));

            verify(loanRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw LoanNotEligibleException when account not owned")
        void shouldThrowWhenAccountNotOwned() {
            LoanApplicationRequest request = new LoanApplicationRequest(
                    LoanType.PERSONAL,
                    BigDecimal.valueOf(50000.00),
                    BigDecimal.valueOf(5.5),
                    60,
                    ACCOUNT_NUMBER
            );

            when(accountServiceClient.isAccountOwned(CUSTOMER_ID, ACCOUNT_NUMBER)).thenReturn(false);

            assertThrows(LoanNotEligibleException.class, () ->
                    loanService.applyForLoan(request, CUSTOMER_ID));

            verify(loanRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should calculate monthly payment with zero interest rate")
        void shouldCalculateMonthlyPaymentWithZeroInterest() {
            LoanApplicationRequest request = new LoanApplicationRequest(
                    LoanType.PERSONAL,
                    BigDecimal.valueOf(12000.00),
                    BigDecimal.ZERO,
                    12,
                    ACCOUNT_NUMBER
            );

            when(accountServiceClient.isAccountOwned(CUSTOMER_ID, ACCOUNT_NUMBER)).thenReturn(true);
            when(loanRepository.existsByCustomerIdAndStatusIn(eq(CUSTOMER_ID), anyList())).thenReturn(false);
            when(loanRepository.existsByLoanNumber(anyString())).thenReturn(false);
            when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
                Loan loan = invocation.getArgument(0);
                loan.setId(UUID.randomUUID());
                return loan;
            });

            LoanResponse response = loanService.applyForLoan(request, CUSTOMER_ID);

            assertNotNull(response);
            assertEquals(new BigDecimal("1000.00"), response.monthlyPayment());
            assertEquals(new BigDecimal("12000.00"), response.totalAmount());
        }
    }

    // ==================== getMyLoans Tests ====================

    @Nested
    @DisplayName("getMyLoans")
    class GetMyLoansTests {

        @Test
        @DisplayName("Should return customer loans")
        void shouldReturnCustomerLoans() {
            Loan loan = buildLoan(LoanStatus.ACTIVE);
            when(loanRepository.findAllByCustomerId(CUSTOMER_ID)).thenReturn(List.of(loan));

            List<LoanInfo> result = loanService.getMyLoans(CUSTOMER_ID);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(LOAN_NUMBER, result.get(0).loanNumber());
        }

        @Test
        @DisplayName("Should return empty list when no loans exist")
        void shouldReturnEmptyListWhenNoLoans() {
            when(loanRepository.findAllByCustomerId(CUSTOMER_ID)).thenReturn(List.of());

            List<LoanInfo> result = loanService.getMyLoans(CUSTOMER_ID);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==================== getMyLoanByNumber Tests ====================

    @Nested
    @DisplayName("getMyLoanByNumber")
    class GetMyLoanByNumberTests {

        @Test
        @DisplayName("Should return loan when owned by customer")
        void shouldReturnLoanWhenOwned() {
            Loan loan = buildLoan(LoanStatus.ACTIVE);
            when(loanRepository.findByLoanNumber(LOAN_NUMBER)).thenReturn(Optional.of(loan));

            LoanResponse response = loanService.getMyLoanByNumber(CUSTOMER_ID, LOAN_NUMBER);

            assertNotNull(response);
            assertEquals(LOAN_NUMBER, response.loanNumber());
        }

        @Test
        @DisplayName("Should throw LoanNotFoundException when loan not found")
        void shouldThrowWhenLoanNotFound() {
            when(loanRepository.findByLoanNumber(LOAN_NUMBER)).thenReturn(Optional.empty());

            assertThrows(LoanNotFoundException.class, () ->
                    loanService.getMyLoanByNumber(CUSTOMER_ID, LOAN_NUMBER));
        }

        @Test
        @DisplayName("Should throw LoanOwnershipException when loan belongs to another customer")
        void shouldThrowWhenLoanNotOwned() {
            Loan loan = buildLoan(LoanStatus.ACTIVE);
            loan.setCustomerId(999L);
            when(loanRepository.findByLoanNumber(LOAN_NUMBER)).thenReturn(Optional.of(loan));

            assertThrows(LoanOwnershipException.class, () ->
                    loanService.getMyLoanByNumber(CUSTOMER_ID, LOAN_NUMBER));
        }
    }

    // ==================== getLoanPayments Tests ====================

    @Nested
    @DisplayName("getLoanPayments")
    class GetLoanPaymentsTests {

        @Test
        @DisplayName("Should return payment schedule for owned loan")
        void shouldReturnPaymentSchedule() {
            Loan loan = buildLoan(LoanStatus.ACTIVE);
            LoanPayment payment = LoanPayment.builder()
                    .id(UUID.randomUUID())
                    .loanId(loan.getId())
                    .paymentNumber(1)
                    .amountDue(BigDecimal.valueOf(955.06))
                    .principalPortion(BigDecimal.valueOf(734.23))
                    .interestPortion(BigDecimal.valueOf(220.83))
                    .dueDate(LocalDate.now().plusMonths(1))
                    .status(LoanPaymentStatus.PENDING)
                    .build();

            when(loanRepository.findByLoanNumber(LOAN_NUMBER)).thenReturn(Optional.of(loan));
            when(loanPaymentRepository.findAllByLoanIdOrderByPaymentNumberAsc(loan.getId()))
                    .thenReturn(List.of(payment));

            List<LoanPaymentResponse> result = loanService.getLoanPayments(CUSTOMER_ID, LOAN_NUMBER);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(1, result.get(0).paymentNumber());
            assertEquals(LoanPaymentStatus.PENDING, result.get(0).status());
        }

        @Test
        @DisplayName("Should throw LoanNotFoundException when loan not found")
        void shouldThrowWhenLoanNotFound() {
            when(loanRepository.findByLoanNumber(LOAN_NUMBER)).thenReturn(Optional.empty());

            assertThrows(LoanNotFoundException.class, () ->
                    loanService.getLoanPayments(CUSTOMER_ID, LOAN_NUMBER));
        }

        @Test
        @DisplayName("Should throw LoanOwnershipException when loan not owned")
        void shouldThrowWhenLoanNotOwned() {
            Loan loan = buildLoan(LoanStatus.ACTIVE);
            loan.setCustomerId(999L);
            when(loanRepository.findByLoanNumber(LOAN_NUMBER)).thenReturn(Optional.of(loan));

            assertThrows(LoanOwnershipException.class, () ->
                    loanService.getLoanPayments(CUSTOMER_ID, LOAN_NUMBER));
        }
    }

    // ==================== makeRepayment Tests ====================

    @Nested
    @DisplayName("makeRepayment")
    class MakeRepaymentTests {

        @Test
        @DisplayName("Should process repayment successfully")
        void shouldProcessRepayment() {
            Loan loan = buildLoan(LoanStatus.ACTIVE);
            LoanRepaymentRequest request = new LoanRepaymentRequest(LOAN_NUMBER, BigDecimal.valueOf(955.06));
            LoanPayment pendingPayment = LoanPayment.builder()
                    .id(UUID.randomUUID())
                    .loanId(loan.getId())
                    .paymentNumber(1)
                    .amountDue(BigDecimal.valueOf(955.06))
                    .principalPortion(BigDecimal.valueOf(734.23))
                    .interestPortion(BigDecimal.valueOf(220.83))
                    .dueDate(LocalDate.now().plusMonths(1))
                    .status(LoanPaymentStatus.PENDING)
                    .build();

            when(loanRepository.findByLoanNumberForUpdate(LOAN_NUMBER)).thenReturn(Optional.of(loan));
            when(loanPaymentRepository.findAllByLoanIdAndStatus(loan.getId(), LoanPaymentStatus.PENDING))
                    .thenReturn(List.of(pendingPayment));
            when(loanPaymentRepository.findByLoanIdAndPaymentNumberForUpdate(loan.getId(), 1))
                    .thenReturn(Optional.of(pendingPayment));
            when(loanPaymentRepository.save(any(LoanPayment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

            loanService.makeRepayment(CUSTOMER_ID, request);

            verify(loanPaymentRepository).save(any(LoanPayment.class));
            verify(loanRepository).save(any(Loan.class));
        }

        @Test
        @DisplayName("Should throw LoanNotFoundException when loan not found")
        void shouldThrowWhenLoanNotFound() {
            LoanRepaymentRequest request = new LoanRepaymentRequest(LOAN_NUMBER, BigDecimal.valueOf(955.06));
            when(loanRepository.findByLoanNumberForUpdate(LOAN_NUMBER)).thenReturn(Optional.empty());

            assertThrows(LoanNotFoundException.class, () ->
                    loanService.makeRepayment(CUSTOMER_ID, request));
        }

        @Test
        @DisplayName("Should throw LoanOwnershipException when loan not owned")
        void shouldThrowWhenLoanNotOwned() {
            Loan loan = buildLoan(LoanStatus.ACTIVE);
            loan.setCustomerId(999L);
            LoanRepaymentRequest request = new LoanRepaymentRequest(LOAN_NUMBER, BigDecimal.valueOf(955.06));
            when(loanRepository.findByLoanNumberForUpdate(LOAN_NUMBER)).thenReturn(Optional.of(loan));

            assertThrows(LoanOwnershipException.class, () ->
                    loanService.makeRepayment(CUSTOMER_ID, request));
        }

        @Test
        @DisplayName("Should throw LoanNotEligibleException when loan is not active")
        void shouldThrowWhenLoanNotActive() {
            Loan loan = buildLoan(LoanStatus.PENDING);
            LoanRepaymentRequest request = new LoanRepaymentRequest(LOAN_NUMBER, BigDecimal.valueOf(955.06));
            when(loanRepository.findByLoanNumberForUpdate(LOAN_NUMBER)).thenReturn(Optional.of(loan));

            assertThrows(LoanNotEligibleException.class, () ->
                    loanService.makeRepayment(CUSTOMER_ID, request));
        }

        @Test
        @DisplayName("Should throw InsufficientRepaymentAmountException when amount less than monthly payment")
        void shouldThrowWhenAmountInsufficient() {
            Loan loan = buildLoan(LoanStatus.ACTIVE);
            LoanRepaymentRequest request = new LoanRepaymentRequest(LOAN_NUMBER, BigDecimal.valueOf(100.00));
            when(loanRepository.findByLoanNumberForUpdate(LOAN_NUMBER)).thenReturn(Optional.of(loan));

            assertThrows(InsufficientRepaymentAmountException.class, () ->
                    loanService.makeRepayment(CUSTOMER_ID, request));
        }

        @Test
        @DisplayName("Should set loan to PAID_OFF when remaining balance reaches zero")
        void shouldSetLoanToPaidOffWhenBalanceZero() {
            Loan loan = buildLoan(LoanStatus.ACTIVE);
            loan.setRemainingBalance(BigDecimal.valueOf(955.06));
            LoanRepaymentRequest request = new LoanRepaymentRequest(LOAN_NUMBER, BigDecimal.valueOf(955.06));
            LoanPayment pendingPayment = LoanPayment.builder()
                    .id(UUID.randomUUID())
                    .loanId(loan.getId())
                    .paymentNumber(1)
                    .amountDue(BigDecimal.valueOf(955.06))
                    .principalPortion(BigDecimal.valueOf(734.23))
                    .interestPortion(BigDecimal.valueOf(220.83))
                    .dueDate(LocalDate.now().plusMonths(1))
                    .status(LoanPaymentStatus.PENDING)
                    .build();

            when(loanRepository.findByLoanNumberForUpdate(LOAN_NUMBER)).thenReturn(Optional.of(loan));
            when(loanPaymentRepository.findAllByLoanIdAndStatus(loan.getId(), LoanPaymentStatus.PENDING))
                    .thenReturn(List.of(pendingPayment));
            when(loanPaymentRepository.findByLoanIdAndPaymentNumberForUpdate(loan.getId(), 1))
                    .thenReturn(Optional.of(pendingPayment));
            when(loanPaymentRepository.save(any(LoanPayment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

            loanService.makeRepayment(CUSTOMER_ID, request);

            ArgumentCaptor<Loan> captor = ArgumentCaptor.forClass(Loan.class);
            verify(loanRepository).save(captor.capture());
            assertEquals(LoanStatus.PAID_OFF, captor.getValue().getStatus());
            assertEquals(BigDecimal.ZERO, captor.getValue().getRemainingBalance());
        }
    }

    // ==================== approveLoan Tests ====================

    @Nested
    @DisplayName("approveLoan")
    class ApproveLoanTests {

        @Test
        @DisplayName("Should approve pending loan")
        void shouldApprovePendingLoan() {
            Loan loan = buildLoan(LoanStatus.PENDING);
            when(loanRepository.findByLoanNumberForUpdate(LOAN_NUMBER)).thenReturn(Optional.of(loan));
            when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

            LoanResponse response = loanService.approveLoan(LOAN_NUMBER);

            assertNotNull(response);
            assertEquals(LoanStatus.APPROVED, response.status());
        }

        @Test
        @DisplayName("Should throw LoanNotFoundException when loan not found")
        void shouldThrowWhenLoanNotFound() {
            when(loanRepository.findByLoanNumberForUpdate(LOAN_NUMBER)).thenReturn(Optional.empty());

            assertThrows(LoanNotFoundException.class, () ->
                    loanService.approveLoan(LOAN_NUMBER));
        }

        @Test
        @DisplayName("Should throw LoanNotEligibleException when loan is not pending")
        void shouldThrowWhenLoanNotPending() {
            Loan loan = buildLoan(LoanStatus.APPROVED);
            when(loanRepository.findByLoanNumberForUpdate(LOAN_NUMBER)).thenReturn(Optional.of(loan));

            assertThrows(LoanNotEligibleException.class, () ->
                    loanService.approveLoan(LOAN_NUMBER));
        }
    }

    // ==================== rejectLoan Tests ====================

    @Nested
    @DisplayName("rejectLoan")
    class RejectLoanTests {

        @Test
        @DisplayName("Should reject pending loan with reason")
        void shouldRejectPendingLoan() {
            Loan loan = buildLoan(LoanStatus.PENDING);
            String reason = "Insufficient credit score";
            when(loanRepository.findByLoanNumberForUpdate(LOAN_NUMBER)).thenReturn(Optional.of(loan));
            when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

            LoanResponse response = loanService.rejectLoan(LOAN_NUMBER, reason);

            assertNotNull(response);
            assertEquals(LoanStatus.REJECTED, response.status());
        }

        @Test
        @DisplayName("Should throw LoanNotFoundException when loan not found")
        void shouldThrowWhenLoanNotFound() {
            when(loanRepository.findByLoanNumberForUpdate(LOAN_NUMBER)).thenReturn(Optional.empty());

            assertThrows(LoanNotFoundException.class, () ->
                    loanService.rejectLoan(LOAN_NUMBER, "reason"));
        }

        @Test
        @DisplayName("Should throw LoanNotEligibleException when loan is not pending")
        void shouldThrowWhenLoanNotPending() {
            Loan loan = buildLoan(LoanStatus.APPROVED);
            when(loanRepository.findByLoanNumberForUpdate(LOAN_NUMBER)).thenReturn(Optional.of(loan));

            assertThrows(LoanNotEligibleException.class, () ->
                    loanService.rejectLoan(LOAN_NUMBER, "reason"));
        }
    }

    // ==================== disburseLoan Tests ====================

    @Nested
    @DisplayName("disburseLoan")
    class DisburseLoanTests {

        @Test
        @DisplayName("Should disburse approved loan")
        void shouldDisburseApprovedLoan() {
            Loan loan = buildLoan(LoanStatus.APPROVED);
            when(loanRepository.findByLoanNumberForUpdate(LOAN_NUMBER)).thenReturn(Optional.of(loan));
            when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(accountServiceClient).deposit(anyLong(), anyString(), any(BigDecimal.class));
            when(loanPaymentRepository.saveAll(anyList())).thenReturn(List.of());

            LoanResponse response = loanService.disburseLoan(LOAN_NUMBER);

            assertNotNull(response);
            assertEquals(LoanStatus.ACTIVE, response.status());
            assertNotNull(response.disbursedAt());
            assertNotNull(response.maturityDate());
            verify(accountServiceClient).deposit(anyLong(), eq(ACCOUNT_NUMBER), any(BigDecimal.class));
            verify(loanPaymentRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Should throw LoanNotFoundException when loan not found")
        void shouldThrowWhenLoanNotFound() {
            when(loanRepository.findByLoanNumberForUpdate(LOAN_NUMBER)).thenReturn(Optional.empty());

            assertThrows(LoanNotFoundException.class, () ->
                    loanService.disburseLoan(LOAN_NUMBER));
        }

        @Test
        @DisplayName("Should throw LoanNotDisbursedException when loan is not approved")
        void shouldThrowWhenLoanNotApproved() {
            Loan loan = buildLoan(LoanStatus.PENDING);
            when(loanRepository.findByLoanNumberForUpdate(LOAN_NUMBER)).thenReturn(Optional.of(loan));

            assertThrows(LoanNotDisbursedException.class, () ->
                    loanService.disburseLoan(LOAN_NUMBER));
        }
    }

    // ==================== getAllLoans Tests ====================

    @Nested
    @DisplayName("getAllLoans")
    class GetAllLoansTests {

        @Test
        @DisplayName("Should return all loans")
        void shouldReturnAllLoans() {
            Loan loan1 = buildLoan(LoanStatus.ACTIVE);
            Loan loan2 = buildLoan(LoanStatus.PENDING);
            loan2.setLoanNumber("LN-9999999999");
            when(loanRepository.findAll()).thenReturn(List.of(loan1, loan2));

            List<LoanResponse> result = loanService.getAllLoans();

            assertNotNull(result);
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("Should return empty list when no loans exist")
        void shouldReturnEmptyList() {
            when(loanRepository.findAll()).thenReturn(List.of());

            List<LoanResponse> result = loanService.getAllLoans();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==================== getLoanByNumber Tests ====================

    @Nested
    @DisplayName("getLoanByNumber")
    class GetLoanByNumberTests {

        @Test
        @DisplayName("Should return loan by number")
        void shouldReturnLoanByNumber() {
            Loan loan = buildLoan(LoanStatus.ACTIVE);
            when(loanRepository.findByLoanNumber(LOAN_NUMBER)).thenReturn(Optional.of(loan));

            LoanResponse response = loanService.getLoanByNumber(LOAN_NUMBER);

            assertNotNull(response);
            assertEquals(LOAN_NUMBER, response.loanNumber());
        }

        @Test
        @DisplayName("Should throw LoanNotFoundException when loan not found")
        void shouldThrowWhenLoanNotFound() {
            when(loanRepository.findByLoanNumber(LOAN_NUMBER)).thenReturn(Optional.empty());

            assertThrows(LoanNotFoundException.class, () ->
                    loanService.getLoanByNumber(LOAN_NUMBER));
        }
    }

    // ==================== getLoansByCustomerId Tests ====================

    @Nested
    @DisplayName("getLoansByCustomerId")
    class GetLoansByCustomerIdTests {

        @Test
        @DisplayName("Should return loans by customer ID")
        void shouldReturnLoansByCustomerId() {
            Loan loan = buildLoan(LoanStatus.ACTIVE);
            when(loanRepository.findAllByCustomerId(CUSTOMER_ID)).thenReturn(List.of(loan));

            List<LoanResponse> result = loanService.getLoansByCustomerId(CUSTOMER_ID);

            assertNotNull(result);
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should return empty list when no loans found")
        void shouldReturnEmptyList() {
            when(loanRepository.findAllByCustomerId(CUSTOMER_ID)).thenReturn(List.of());

            List<LoanResponse> result = loanService.getLoansByCustomerId(CUSTOMER_ID);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==================== hasActiveLoans Tests ====================

    @Nested
    @DisplayName("hasActiveLoans")
    class HasActiveLoansTests {

        @Test
        @DisplayName("Should return true when customer has active loans")
        void shouldReturnTrueWhenHasActiveLoans() {
            when(loanRepository.existsByCustomerIdAndStatusIn(eq(CUSTOMER_ID), anyList())).thenReturn(true);

            boolean result = loanService.hasActiveLoans(CUSTOMER_ID);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when customer has no active loans")
        void shouldReturnFalseWhenNoActiveLoans() {
            when(loanRepository.existsByCustomerIdAndStatusIn(eq(CUSTOMER_ID), anyList())).thenReturn(false);

            boolean result = loanService.hasActiveLoans(CUSTOMER_ID);

            assertFalse(result);
        }
    }

    // ==================== processDisbursement Tests ====================

    @Nested
    @DisplayName("processDisbursement")
    class ProcessDisbursementTests {

        @Test
        @DisplayName("Should process internal disbursement successfully")
        void shouldProcessInternalDisbursement() {
            Loan loan = buildLoan(LoanStatus.APPROVED);
            BigDecimal amount = BigDecimal.valueOf(50000.00);
            when(loanRepository.findByLoanNumberForUpdate(LOAN_NUMBER)).thenReturn(Optional.of(loan));
            doNothing().when(accountServiceClient).deposit(loan.getCustomerId(), ACCOUNT_NUMBER, amount);
            when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));
            when(loanPaymentRepository.saveAll(anyList())).thenReturn(List.of());

            loanService.processDisbursement(CUSTOMER_ID, LOAN_NUMBER, amount, ACCOUNT_NUMBER);

            verify(accountServiceClient).deposit(loan.getCustomerId(), ACCOUNT_NUMBER, amount);
            verify(loanRepository).save(any(Loan.class));
            verify(loanPaymentRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Should throw LoanNotFoundException when loan not found")
        void shouldThrowWhenLoanNotFound() {
            when(loanRepository.findByLoanNumberForUpdate(LOAN_NUMBER)).thenReturn(Optional.empty());

            assertThrows(LoanNotFoundException.class, () ->
                    loanService.processDisbursement(CUSTOMER_ID, LOAN_NUMBER, BigDecimal.valueOf(50000.00), ACCOUNT_NUMBER));
        }

        @Test
        @DisplayName("Should throw LoanNotDisbursedException when loan is not approved")
        void shouldThrowWhenLoanNotApproved() {
            Loan loan = buildLoan(LoanStatus.PENDING);
            when(loanRepository.findByLoanNumberForUpdate(LOAN_NUMBER)).thenReturn(Optional.of(loan));

            assertThrows(LoanNotDisbursedException.class, () ->
                    loanService.processDisbursement(CUSTOMER_ID, LOAN_NUMBER, BigDecimal.valueOf(50000.00), ACCOUNT_NUMBER));
        }
    }

    // ==================== processRepayment Tests ====================

    @Nested
    @DisplayName("processRepayment")
    class ProcessRepaymentTests {

        @Test
        @DisplayName("Should process internal repayment successfully")
        void shouldProcessInternalRepayment() {
            Loan loan = buildLoan(LoanStatus.ACTIVE);
            BigDecimal amount = BigDecimal.valueOf(955.06);
            LoanPayment pendingPayment = LoanPayment.builder()
                    .id(UUID.randomUUID())
                    .loanId(loan.getId())
                    .paymentNumber(1)
                    .amountDue(BigDecimal.valueOf(955.06))
                    .principalPortion(BigDecimal.valueOf(734.23))
                    .interestPortion(BigDecimal.valueOf(220.83))
                    .dueDate(LocalDate.now().plusMonths(1))
                    .status(LoanPaymentStatus.PENDING)
                    .build();

            when(loanRepository.findByLoanNumberForUpdate(LOAN_NUMBER)).thenReturn(Optional.of(loan));
            when(loanPaymentRepository.findAllByLoanIdAndStatus(loan.getId(), LoanPaymentStatus.PENDING))
                    .thenReturn(List.of(pendingPayment));
            when(loanPaymentRepository.findByLoanIdAndPaymentNumberForUpdate(loan.getId(), 1))
                    .thenReturn(Optional.of(pendingPayment));
            when(loanPaymentRepository.save(any(LoanPayment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

            loanService.processRepayment(LOAN_NUMBER, amount);

            verify(loanPaymentRepository).save(any(LoanPayment.class));
            verify(loanRepository).save(any(Loan.class));
        }



        @Test
        @DisplayName("Should throw LoanNotFoundException when loan not found")
        void shouldThrowWhenLoanNotFound() {
            when(loanRepository.findByLoanNumberForUpdate(LOAN_NUMBER)).thenReturn(Optional.empty());

            assertThrows(LoanNotFoundException.class, () ->
                    loanService.processRepayment(LOAN_NUMBER, BigDecimal.valueOf(955.06)));
        }

        @Test
        @DisplayName("Should throw LoanNotEligibleException when loan is not active or disbursed")
        void shouldThrowWhenLoanNotEligible() {
            Loan loan = buildLoan(LoanStatus.PENDING);
            when(loanRepository.findByLoanNumberForUpdate(LOAN_NUMBER)).thenReturn(Optional.of(loan));

            assertThrows(LoanNotEligibleException.class, () ->
                    loanService.processRepayment(LOAN_NUMBER, BigDecimal.valueOf(955.06)));
        }
    }
}
