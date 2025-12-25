package com.amerbank.transaction.service;

import com.amerbank.common_dto.*;
import com.amerbank.transaction.dto.DepositTransactionRequest;
import com.amerbank.transaction.dto.PaymentTransactionRequest;
import com.amerbank.transaction.dto.RefundTransactionRequest;
import com.amerbank.transaction.dto.TransactionResponse;
import com.amerbank.transaction.exception.*;
import com.amerbank.transaction.model.Transaction;
import com.amerbank.transaction.model.TransactionStatus;
import com.amerbank.transaction.model.TransactionType;
import com.amerbank.transaction.security.JwtService;
import com.amerbank.transaction.repository.TransactionRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Service
@AllArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final RestTemplate restTemplate;
    private final JwtService jwtService;
    private final IdempotencyService idempotencyService;

    public Transaction findTransactionById(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found"));
    }

    public List<Transaction> findTransactionsByFromAccountNumber(String fromAccount) {
        return transactionRepository.findByFromAccountNumber(fromAccount);
    }

    public List<Transaction> findTransactionsByToAccountNumber(String toAccount) {
        return transactionRepository.findByToAccountNumber(toAccount);
    }

    public List<Transaction> findTransactionsByFromAndToAccountNumber(String fromAccount, String toAccount) {
        return transactionRepository.findByFromAccountNumberAndToAccountNumber(fromAccount, toAccount);
    }

    public List<Transaction> findTransactionsByStatus(TransactionStatus status) {
        return transactionRepository.findByStatus(status);
    }

    public List<Transaction> findTransactionsByType(TransactionType type) {
        return transactionRepository.findByType(type);
    }

    public List<Transaction> findByFromAccountNumberAndStatus(String fromAccount, TransactionStatus status) {
        return transactionRepository.findByFromAccountNumberAndStatus(fromAccount, status);
    }

    public List<Transaction> findByFromAccountNumberOrToAccountNumber(String accountNumber) {
        return transactionRepository.findByFromAccountNumberOrToAccountNumber(accountNumber, accountNumber);
    }

    public List<Transaction> findByFromAccountNumberAndType(String fromAccount, TransactionType type) {
        return transactionRepository.findByFromAccountNumberAndType(fromAccount, type);
    }

    public List<Transaction> getMyTransactions(String jwtToken, String accountNumber) {
        Long customerId = jwtService.extractCustomerId(jwtToken);
        if (!isAccountOwnedByCurrentCustomer(accountNumber, customerId)) {
            throw new UnauthorizedAccountAccessException("Account does not belong to current User");
        }
        return findByFromAccountNumberOrToAccountNumber(accountNumber);
    }

    public TransactionResponse createDepositTransaction(
            String jwtToken,
            String idempotencyKey,
            DepositTransactionRequest request
    ) {

        Long customerId = jwtService.extractCustomerId(jwtToken);

        return idempotencyService.execute(
                idempotencyKey,
                () -> transactionMapper.toTransaction(request),
                tx -> performDeposit(customerId, request.toAccountNumber(), request.amount()),
                transactionMapper::toResponse
        );
    }

    public TransactionResponse createPaymentTransaction(
            String jwtToken,
            String idempotencyKey,
            PaymentTransactionRequest request
    ) {

        Long customerId = jwtService.extractCustomerId(jwtToken);

        return idempotencyService.execute(
                idempotencyKey,
                () -> transactionMapper.toTransaction(request),
                tx -> performPayment(
                        customerId,
                        request.fromAccountNumber(),
                        request.toAccountNumber(),
                        request.amount()
                ),
                transactionMapper::toResponse
        );
    }

    public TransactionResponse createRefundTransaction(
            String jwtToken,
            String idempotencyKey,
            RefundTransactionRequest request
    ) {

        Transaction original = findTransactionById(request.transactionId());

        if (original.getStatus() == TransactionStatus.REVERSED) {
            throw new TransactionAlreadyRefundedException("Transaction already refunded");
        }

        Long customerId = jwtService.extractCustomerId(jwtToken);

        return idempotencyService.execute(
                idempotencyKey,
                () -> {
                    Transaction tx = new Transaction();
                    tx.setAmount(original.getAmount());
                    tx.setFromAccountNumber(original.getToAccountNumber());
                    tx.setToAccountNumber(original.getFromAccountNumber());
                    tx.setType(TransactionType.REFUND);
                    return tx;
                },
                tx -> {
                    performRefund(
                            customerId,
                            tx.getFromAccountNumber(),
                            tx.getToAccountNumber(),
                            tx.getAmount()
                    );
                    original.setStatus(TransactionStatus.REVERSED);
                    transactionRepository.save(original);
                },
                transactionMapper::toResponse
        );

    }

    private boolean isAccountOwnedByCurrentCustomer(String accountNumber, Long customerId) {
        String url = "http://account/accounts/internal/owned";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtService.generateServiceToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ServiceAccountOwnedRequest> entity =
                new HttpEntity<>(new ServiceAccountOwnedRequest(customerId, accountNumber), headers);

        try {
            return Boolean.TRUE.equals(
                    restTemplate.exchange(url, HttpMethod.POST, entity, Boolean.class).getBody()
            );
        } catch (HttpClientErrorException e) {
            throw new AccountServiceUnavailableException("An error occurred." + e.getStatusCode());
        } catch (RestClientException e) {
            throw new AccountServiceUnavailableException("Could not reach account service");
        }
    }

    private void performDeposit(Long customerId, String accountNumber, BigDecimal amount) {
        executeAccountCall(
                "http://account/accounts/internal/deposit",
                new ServiceDepositBalanceRequest(customerId, accountNumber, amount),
                DepositFailedException::new
        );
    }

    private void performPayment(Long customerId, String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        executeAccountCall(
                "http://account/accounts/internal/payment",
                new ServicePaymentRequest(customerId, fromAccountNumber, toAccountNumber, amount),
                PaymentFailedException::new
        );
    }

    private void performRefund(Long customerId, String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        executeAccountCall(
                "http://account/accounts/internal/refund",
                new ServiceRefundBalanceRequest(customerId, toAccountNumber, fromAccountNumber, amount),
                RefundFailedException::new
        );
    }

    private void executeAccountCall(
            String url,
            Object body,
            Function<String, RuntimeException> exceptionFactory
    ) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtService.generateServiceToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Void.class);
        } catch (HttpClientErrorException e) {
            throw exceptionFactory.apply("Rejected: " + e.getStatusCode());
        } catch (RestClientException e) {
            throw exceptionFactory.apply("Account service unavailable");
        }
    }
}