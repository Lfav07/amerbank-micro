package com.amerbank.transaction.service;

import com.amerbank.transaction.config.TransactionProperties;
import com.amerbank.transaction.dto.*;
import com.amerbank.transaction.exception.*;
import com.amerbank.transaction.model.Transaction;
import com.amerbank.transaction.model.TransactionStatus;
import com.amerbank.transaction.model.TransactionType;
import com.amerbank.transaction.security.JwtService;
import com.amerbank.transaction.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Service
@Slf4j
public class TransactionService {



    private final TransactionRepository transactionRepository;
    private final TransactionProperties transactionProperties;
    private final TransactionMapper transactionMapper;
    private final RestClient restClient;
    private final JwtService jwtService;
    private final IdempotencyService idempotencyService;

    public TransactionService(
            TransactionRepository transactionRepository, TransactionProperties transactionProperties,
            TransactionMapper transactionMapper,
            RestClient.Builder restClientBuilder,
            JwtService jwtService,
            IdempotencyService idempotencyService
    ) {
        this.transactionRepository = transactionRepository;
        this.transactionProperties = transactionProperties;
        this.transactionMapper = transactionMapper;
        this.restClient = restClientBuilder.build();
        this.jwtService = jwtService;
        this.idempotencyService = idempotencyService;
    }

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

    public List<Transaction> getMyTransactions(Long customerId, String accountNumber) {

        if (!isAccountOwnedByCurrentCustomer(accountNumber, customerId)) {
            throw new UnauthorizedAccountAccessException("Account does not belong to current User");
        }
        return findByFromAccountNumberOrToAccountNumber(accountNumber);
    }

    public TransactionResponse createDepositTransaction(
            Long customerId,
            String idempotencyKey,
            DepositTransactionRequest request
    ) {
        log.info("Creating deposit transaction - customerId: {}, toAccount: {}, amount: {}, idempotencyKey: {}",
                customerId, request.toAccountNumber(), request.amount(), idempotencyKey);

        return idempotencyService.execute(
                idempotencyKey,
                () -> transactionMapper.toTransaction(request),
                tx -> performDeposit(customerId, request.toAccountNumber(), request.amount()),
                transactionMapper::toResponse
        );
    }

    public TransactionResponse createPaymentTransaction(
            Long customerId,
            String idempotencyKey,
            PaymentTransactionRequest request
    ) {
        log.info("Creating payment transaction - customerId: {}, fromAccount: {}, toAccount: {}, amount: {}, idempotencyKey: {}",
                customerId, request.fromAccountNumber(), request.toAccountNumber(), request.amount(), idempotencyKey);

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
            Long customerId,
            String idempotencyKey,
            RefundTransactionRequest request
    ) {
        log.info("Creating refund transaction - customerId: {}, transactionId: {}, idempotencyKey: {}",
                customerId, request.transactionId(), idempotencyKey);

        Transaction original = findTransactionById(request.transactionId());

        if (original.getStatus() == TransactionStatus.REVERSED) {
            throw new TransactionAlreadyRefundedException("Transaction already refunded");
        }

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
        String url = transactionProperties.getAccountServiceBase() + transactionProperties.getEndpointOwned();
        String serviceToken = jwtService.generateServiceToken();
        ServiceAccountOwnedRequest requestBody =
                new ServiceAccountOwnedRequest(customerId, accountNumber);

        try {
            Boolean result = restClient
                    .post()
                    .uri(url)
                    .headers(h -> h.setBearerAuth(serviceToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Boolean.class);
            log.debug("Successfully verified account ownership - accountNumber: {}, customerId: {}, owned: {}",
                    accountNumber, customerId, result);
            return Boolean.TRUE.equals(result);
        } catch (HttpClientErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            log.error("Account ownership check failed - url: {}, status: {}, accountNumber: {}, customerId: {}, responseBody: {}",
                    url, e.getStatusCode(), accountNumber, customerId, responseBody);
            throw new AccountServiceUnavailableException(
                    String.format("Account ownership check failed. Status: %s, URL: %s", e.getStatusCode(), url)
            );
        } catch (RestClientException e) {
            log.error("Could not reach account service - url: {}, accountNumber: {}, customerId: {}, error: {}",
                    url, accountNumber, customerId, e.getMessage());
            throw new AccountServiceUnavailableException(
                    String.format("Could not reach account service. URL: %s", url)
            );
        }
    }

    private void performDeposit(Long customerId, String accountNumber, BigDecimal amount) {
        executeAccountCall(
                transactionProperties.getEndpointDeposit(),
                new ServiceDepositBalanceRequest(customerId, accountNumber, amount),
                DepositFailedException::new
        );
    }

    private void performPayment(Long customerId, String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        executeAccountCall(
                transactionProperties.getEndpointPayment(),
                new ServicePaymentRequest(customerId, fromAccountNumber, toAccountNumber, amount),
                PaymentFailedException::new
        );
    }

    private void performRefund(Long customerId, String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        executeAccountCall(
                transactionProperties.getEndpointRefund(),
                new ServiceRefundBalanceRequest(customerId, toAccountNumber, fromAccountNumber, amount),
                RefundFailedException::new
        );
    }

    private void executeAccountCall(
            String endpoint,
            Object body,
            Function<String, RuntimeException> exceptionFactory
    ) {
        String url = transactionProperties.getAccountServiceBase() + endpoint;
        String serviceToken = jwtService.generateServiceToken();

        try {
            restClient
                    .post()
                    .uri(url)
                    .headers(h -> h.setBearerAuth(serviceToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Successfully executed account call - endpoint: {}, url: {}", endpoint, url);
        } catch (HttpClientErrorException e) {
            String responseBody = e.getResponseBodyAsString();

            log.error("Account service call rejected - endpoint: {}, url: {}, status: {}, responseBody: {}",
                    endpoint, url, e.getStatusCode(), responseBody);

            throw exceptionFactory.apply(
                    String.format("Account service rejected request. Status: %s, URL: %s", e.getStatusCode(), url)
            );
        } catch (RestClientException e) {

            log.error("Account service unavailable - endpoint: {}, url: {}, error: {}", endpoint, url, e.getMessage());

            throw exceptionFactory.apply(
                    String.format("Account service unavailable. URL: %s", url)
            );
        }
    }
}
