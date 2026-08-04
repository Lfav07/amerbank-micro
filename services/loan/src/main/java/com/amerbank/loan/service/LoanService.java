package com.amerbank.loan.service;

import com.amerbank.loan.client.AccountServiceClientInterface;
import com.amerbank.loan.client.CustomerServiceClientInterface;
import com.amerbank.loan.config.LoanProperties;
import com.amerbank.loan.dto.LoanInfo;
import com.amerbank.loan.dto.request.LoanApplicationRequest;
import com.amerbank.loan.dto.request.LoanRepaymentRequest;
import com.amerbank.loan.dto.response.LoanPaymentResponse;
import com.amerbank.loan.dto.response.LoanResponse;
import com.amerbank.loan.exception.InsufficientRepaymentAmountException;
import com.amerbank.loan.exception.LoanAlreadyCompletedException;
import com.amerbank.loan.exception.LoanAlreadyDisbursedException;
import com.amerbank.loan.exception.LoanAlreadyExistsException;
import com.amerbank.loan.exception.LoanNotDisbursedException;
import com.amerbank.loan.exception.LoanNotFoundException;
import com.amerbank.loan.exception.LoanNotEligibleException;
import com.amerbank.loan.exception.LoanNumberGenerationException;
import com.amerbank.loan.exception.LoanOwnershipException;
import com.amerbank.loan.model.Loan;
import com.amerbank.loan.model.LoanPayment;
import com.amerbank.loan.model.LoanPaymentStatus;
import com.amerbank.loan.model.LoanStatus;
import com.amerbank.loan.repository.LoanPaymentRepository;
import com.amerbank.loan.repository.LoanRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanPaymentRepository loanPaymentRepository;
    private final LoanMapper loanMapper;
    private final AccountServiceClientInterface accountServiceClient;
    private final CustomerServiceClientInterface customerServiceClient;
    private final LoanProperties loanProperties;

    @Transactional
    public LoanResponse applyForLoan(LoanApplicationRequest request, Long customerId) {
        validateAccountOwnership(customerId, request.accountNumber());

        if (loanRepository.existsByCustomerIdAndStatusIn(customerId, List.of(LoanStatus.PENDING, LoanStatus.APPROVED, LoanStatus.DISBURSED, LoanStatus.ACTIVE))) {
            throw new LoanAlreadyExistsException("Customer already has an active or pending loan");
        }

        BigDecimal monthlyPayment = calculateMonthlyPayment(request.principalAmount(), request.interestRate(), request.termMonths());
        BigDecimal totalAmount = calculateTotalAmount(monthlyPayment, request.termMonths());

        Loan loan = Loan.builder()
                .loanNumber(generateLoanNumber())
                .customerId(customerId)
                .accountNumber(request.accountNumber())
                .principalAmount(request.principalAmount())
                .interestRate(request.interestRate())
                .termMonths(request.termMonths())
                .monthlyPayment(monthlyPayment)
                .totalAmount(totalAmount)
                .remainingBalance(totalAmount)
                .type(request.type())
                .status(LoanStatus.PENDING)
                .build();

        Loan savedLoan = loanRepository.save(loan);
        log.info("Loan application created - loanNumber: {}, customerId: {}", savedLoan.getLoanNumber(), customerId);
        return loanMapper.toResponse(savedLoan);
    }

    public List<LoanInfo> getMyLoans(Long customerId) {
        return loanRepository.findAllByCustomerId(customerId).stream()
                .map(loanMapper::toLoanInfo)
                .collect(Collectors.toList());
    }

    public LoanResponse getMyLoanByNumber(Long customerId, String loanNumber) {
        Loan loan = loanRepository.findByLoanNumber(loanNumber)
                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + loanNumber));

        if (!loan.getCustomerId().equals(customerId)) {
            throw new LoanOwnershipException("Loan does not belong to customer");
        }

        return loanMapper.toResponse(loan);
    }

    public List<LoanPaymentResponse> getLoanPayments(Long customerId, String loanNumber) {
        Loan loan = loanRepository.findByLoanNumber(loanNumber)
                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + loanNumber));

        if (!loan.getCustomerId().equals(customerId)) {
            throw new LoanOwnershipException("Loan does not belong to customer");
        }

        return loanPaymentRepository.findAllByLoanIdOrderByPaymentNumberAsc(loan.getId()).stream()
                .map(loanMapper::toPaymentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void makeRepayment(Long customerId, LoanRepaymentRequest request) {
        Loan loan = loanRepository.findByLoanNumberForUpdate(request.loanNumber())
                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + request.loanNumber()));

        if (!loan.getCustomerId().equals(customerId)) {
            throw new LoanOwnershipException("Loan does not belong to customer");
        }

        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new LoanNotEligibleException("Loan is not active");
        }

        if (request.amount().compareTo(loan.getMonthlyPayment()) < 0) {
            throw new InsufficientRepaymentAmountException("Repayment amount must be at least the monthly payment");
        }

        accountServiceClient.withdraw(customerId, loan.getAccountNumber(), request.amount());

        processRepayment(customerId, request.loanNumber(), request.amount());
    }

    @Transactional
    public LoanResponse approveLoan(String loanNumber) {
        Loan loan = loanRepository.findByLoanNumberForUpdate(loanNumber)
                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + loanNumber));

        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new LoanNotEligibleException("Loan is not pending");
        }

        loan.setStatus(LoanStatus.APPROVED);
        Loan savedLoan = loanRepository.save(loan);
        log.info("Loan approved - loanNumber: {}", loanNumber);
        return loanMapper.toResponse(savedLoan);
    }

    @Transactional
    public LoanResponse rejectLoan(String loanNumber, String reason) {
        Loan loan = loanRepository.findByLoanNumberForUpdate(loanNumber)
                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + loanNumber));

        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new LoanNotEligibleException("Loan is not pending");
        }

        loan.setStatus(LoanStatus.REJECTED);
        loan.setRejectionReason(reason);
        Loan savedLoan = loanRepository.save(loan);
        log.info("Loan rejected - loanNumber: {}, reason: {}", loanNumber, reason);
        return loanMapper.toResponse(savedLoan);
    }

    @Transactional
    public LoanResponse disburseLoan(String loanNumber) {
        Loan loan = loanRepository.findByLoanNumberForUpdate(loanNumber)
                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + loanNumber));

        if (loan.getStatus() != LoanStatus.APPROVED) {
            throw new LoanNotDisbursedException("Loan is not approved");
        }

        accountServiceClient.deposit(loan.getCustomerId(), loan.getAccountNumber(), loan.getPrincipalAmount());

        loan.setStatus(LoanStatus.ACTIVE);
        loan.setDisbursedAt(LocalDateTime.now());
        loan.setMaturityDate(LocalDate.now().plusMonths(loan.getTermMonths()));
        Loan savedLoan = loanRepository.save(loan);

        generatePaymentSchedule(savedLoan);
        log.info("Loan disbursed - loanNumber: {}", loanNumber);
        return loanMapper.toResponse(savedLoan);
    }

    public List<LoanResponse> getAllLoans() {
        return loanRepository.findAll().stream()
                .map(loanMapper::toResponse)
                .collect(Collectors.toList());
    }

    public LoanResponse getLoanByNumber(String loanNumber) {
        Loan loan = loanRepository.findByLoanNumber(loanNumber)
                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + loanNumber));
        return loanMapper.toResponse(loan);
    }

    public List<LoanResponse> getLoansByCustomerId(Long customerId) {
        return loanRepository.findAllByCustomerId(customerId).stream()
                .map(loanMapper::toResponse)
                .collect(Collectors.toList());
    }

    public boolean hasActiveLoans(Long customerId) {
        return loanRepository.existsByCustomerIdAndStatusIn(customerId, List.of(LoanStatus.ACTIVE, LoanStatus.DISBURSED));
    }

    @Transactional
    public void processDisbursement(Long customerId, String loanNumber, BigDecimal amount, String accountNumber) {
        Loan loan = loanRepository.findByLoanNumberForUpdate(loanNumber)
                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + loanNumber));

        if (loan.getStatus() != LoanStatus.APPROVED) {
            throw new LoanNotDisbursedException("Loan is not approved");
        }

        accountServiceClient.deposit(customerId, accountNumber, amount);

        loan.setStatus(LoanStatus.ACTIVE);
        loan.setDisbursedAt(LocalDateTime.now());
        loan.setMaturityDate(LocalDate.now().plusMonths(loan.getTermMonths()));
        loanRepository.save(loan);

        generatePaymentSchedule(loan);
        log.info("Loan disbursed via internal API - loanNumber: {}", loanNumber);
    }

    @Transactional
    public void processRepayment(Long customerId, String loanNumber, BigDecimal amount) {
        Loan loan = loanRepository.findByLoanNumberForUpdate(loanNumber)
                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + loanNumber));

        if (loan.getStatus() != LoanStatus.ACTIVE && loan.getStatus() != LoanStatus.DISBURSED) {
            throw new LoanNotEligibleException("Loan is not active or disbursed");
        }
        
        List<LoanPayment> pendingPayments = loanPaymentRepository.findAllByLoanIdAndStatus(loan.getId(), LoanPaymentStatus.PENDING);
        BigDecimal remainingAmount = amount;

        for (LoanPayment payment : pendingPayments) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal paymentAmount = remainingAmount.min(payment.getAmountDue());
            LoanPayment updatedPayment = loanPaymentRepository.findByLoanIdAndPaymentNumberForUpdate(loan.getId(), payment.getPaymentNumber())
                    .orElse(payment);

            updatedPayment.setStatus(LoanPaymentStatus.PAID);
            updatedPayment.setPaidDate(LocalDateTime.now());
            loanPaymentRepository.save(updatedPayment);

            remainingAmount = remainingAmount.subtract(paymentAmount);
        }

        loan.setRemainingBalance(loan.getRemainingBalance().subtract(amount));
        if (loan.getRemainingBalance().compareTo(BigDecimal.ZERO) <= 0) {
            loan.setRemainingBalance(BigDecimal.ZERO);
            loan.setStatus(LoanStatus.PAID_OFF);
        }
        loanRepository.save(loan);
        log.info("Loan repayment processed - loanNumber: {}, amount: {}", loanNumber, amount);
    }

    private String generateLoanNumber() {
        int maxAttempts = loanProperties.getMaxAttempts();
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            long body = ThreadLocalRandom.current().nextLong(loanProperties.getUpperBound());
            String candidate = loanProperties.getPrefix() + "-"
                    + String.format("%0" + loanProperties.getBodyDigits() + "d", body);
            if (!loanRepository.existsByLoanNumber(candidate)) {
                return candidate;
            }
        }
        throw new LoanNumberGenerationException("Unable to generate unique loan number after " + maxAttempts + " attempts");
    }

    private List<LoanPayment> generatePaymentSchedule(Loan loan) {
        List<LoanPayment> payments = new ArrayList<>();
        BigDecimal monthlyRate = loan.getInterestRate().divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)
                .divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);

        BigDecimal balance = loan.getPrincipalAmount();
        LocalDate startDate = LocalDate.now();

        for (int i = 1; i <= loan.getTermMonths(); i++) {
            BigDecimal interestPortion = balance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalPortion = loan.getMonthlyPayment().subtract(interestPortion);
            BigDecimal amountDue = loan.getMonthlyPayment();

            if (i == loan.getTermMonths()) {
                principalPortion = balance;
                amountDue = principalPortion.add(interestPortion);
            }

            LoanPayment payment = LoanPayment.builder()
                    .loanId(loan.getId())
                    .paymentNumber(i)
                    .amountDue(amountDue)
                    .principalPortion(principalPortion)
                    .interestPortion(interestPortion)
                    .dueDate(startDate.plusMonths(i))
                    .status(LoanPaymentStatus.PENDING)
                    .build();

            payments.add(payment);
            balance = balance.subtract(principalPortion);
        }

        loanPaymentRepository.saveAll(payments);
        return payments;
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal annualRate, int termMonths) {
        if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(new BigDecimal(termMonths), 2, RoundingMode.HALF_UP);
        }

        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)
                .divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);
        BigDecimal compoundFactor = monthlyRate.add(BigDecimal.ONE).pow(termMonths);
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(compoundFactor);
        BigDecimal denominator = compoundFactor.subtract(BigDecimal.ONE);
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTotalAmount(BigDecimal monthlyPayment, int termMonths) {
        return monthlyPayment.multiply(new BigDecimal(termMonths)).setScale(2, RoundingMode.HALF_UP);
    }

    private void validateAccountOwnership(Long customerId, String accountNumber) {
        if (!accountServiceClient.isAccountOwned(customerId, accountNumber)) {
            throw new LoanNotEligibleException("Account not owned by customer");
        }
    }
}