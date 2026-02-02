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

/**
 * Service layer for managing banking transactions.
 *
 * <p>This service handles all transaction-related operations including deposits, payments, and refunds.
 * It coordinates with the account service to verify account ownership and execute balance modifications.
 * All transaction operations support idempotency to prevent duplicate processing.</p>
 *
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Transaction retrieval and querying by various criteria</li>
 *   <li>Account ownership verification</li>
 *   <li>Transaction creation with idempotency support</li>
 *   <li>Communication with the account service for balance operations</li>
 * </ul>
 */
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

    /**
     * Retrieves a transaction by its unique identifier.
     *
     * @param id the unique identifier of the transaction
     * @return the transaction entity
     * @throws TransactionNotFoundException if no transaction exists with the given ID
     */
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

    public TransactionResponse getTransactionResponseById(UUID id) {
        return transactionMapper.toResponse(findTransactionById(id));
    }

    public List<TransactionResponse> getTransactionResponsesByFromAccountNumber(String fromAccount) {
        return findTransactionsByFromAccountNumber(fromAccount)
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    public List<TransactionResponse> getTransactionResponsesByToAccountNumber(String toAccount) {
        return findTransactionsByToAccountNumber(toAccount)
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    public List<TransactionResponse> getTransactionResponsesByFromAndToAccountNumber(String fromAccount, String toAccount) {
        return findTransactionsByFromAndToAccountNumber(fromAccount, toAccount)
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    public List<TransactionResponse> getTransactionResponsesByStatus(TransactionStatus status) {
        return findTransactionsByStatus(status)
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    public List<TransactionResponse> getTransactionResponsesByType(TransactionType type) {
        return findTransactionsByType(type)
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    /**
     * Retrieves all transactions associated with an account owned by the specified customer.
     * This includes both transactions where the account is the source or destination.
     *
     * @param customerId the ID of the customer requesting their transactions
     * @param accountNumber the account number to query transactions for
     * @return a list of all transactions involving the specified account
     * @throws UnauthorizedAccountAccessException if the account does not belong to the customer
     */
    public List<Transaction> getMyTransactions(Long customerId, String accountNumber) {

        if (!isAccountOwnedByCurrentCustomer(accountNumber, customerId)) {
            throw new UnauthorizedAccountAccessException("Account does not belong to current User");
        }
        return findByFromAccountNumberOrToAccountNumber(accountNumber);
    }

    /**
     * Retrieves all transactions originating from an account owned by the specified customer.
     *
     * @param customerId the ID of the customer requesting their transactions
     * @param fromAccountNumber the source account number to query transactions for
     * @return a list of transactions originating from the specified account
     * @throws UnauthorizedAccountAccessException if the account does not belong to the customer
     */
    public List<Transaction> getMyTransactionsByFromAccount(Long customerId, String fromAccountNumber) {
        if (!isAccountOwnedByCurrentCustomer(fromAccountNumber, customerId)) {
            throw new UnauthorizedAccountAccessException("Account does not belong to current User");
        }
        return findTransactionsByFromAccountNumber(fromAccountNumber);
    }

    /**
     * Retrieves all transactions received by an account owned by the specified customer.
     *
     * @param customerId the ID of the customer requesting their transactions
     * @param toAccountNumber the destination account number to query transactions for
     * @return a list of transactions received by the specified account
     * @throws UnauthorizedAccountAccessException if the account does not belong to the customer
     */
    public List<Transaction> getMyTransactionsByToAccount(Long customerId, String toAccountNumber) {
        if (!isAccountOwnedByCurrentCustomer(toAccountNumber, customerId)) {
            throw new UnauthorizedAccountAccessException("Account does not belong to current User");
        }
        return findTransactionsByToAccountNumber(toAccountNumber);
    }

    /**
     * Retrieves all transactions between two specific accounts where the source account
     * is owned by the specified customer.
     *
     * @param customerId the ID of the customer requesting their transactions
     * @param fromAccountNumber the source account number (must be owned by the customer)
     * @param toAccountNumber the destination account number
     * @return a list of transactions from the source to the destination account
     * @throws UnauthorizedAccountAccessException if the source account does not belong to the customer
     */
    public List<Transaction> getMyTransactionsByFromAndToAccount(Long customerId, String fromAccountNumber, String toAccountNumber) {
        if (!isAccountOwnedByCurrentCustomer(fromAccountNumber, customerId)) {
            throw new UnauthorizedAccountAccessException("From account does not belong to current User");
        }
        return findTransactionsByFromAndToAccountNumber(fromAccountNumber, toAccountNumber);
    }

    /**
     * Retrieves all transactions with a specific status for an account owned by the specified customer.
     * Only transactions originating from the account are included.
     *
     * @param customerId the ID of the customer requesting their transactions
     * @param accountNumber the account number to query transactions for
     * @param status the transaction status to filter by
     * @return a list of transactions matching the specified status
     * @throws UnauthorizedAccountAccessException if the account does not belong to the customer
     */
    public List<Transaction> getMyTransactionsByStatus(Long customerId, String accountNumber, TransactionStatus status) {
        if (!isAccountOwnedByCurrentCustomer(accountNumber, customerId)) {
            throw new UnauthorizedAccountAccessException("Account does not belong to current User");
        }
        return findByFromAccountNumberAndStatus(accountNumber, status);
    }

    /**
     * Retrieves all transactions of a specific type for an account owned by the specified customer.
     * Only transactions originating from the account are included.
     *
     * @param customerId the ID of the customer requesting their transactions
     * @param accountNumber the account number to query transactions for
     * @param type the transaction type to filter by
     * @return a list of transactions matching the specified type
     * @throws UnauthorizedAccountAccessException if the account does not belong to the customer
     */
    public List<Transaction> getMyTransactionsByType(Long customerId, String accountNumber, TransactionType type) {
        if (!isAccountOwnedByCurrentCustomer(accountNumber, customerId)) {
            throw new UnauthorizedAccountAccessException("Account does not belong to current User");
        }
        return findByFromAccountNumberAndType(accountNumber, type);
    }

    /**
     * Retrieves all transactions associated with an account owned by the specified customer,
     * returning them as DTOs.
     *
     * @param customerId the ID of the customer requesting their transactions
     * @param accountNumber the account number to query transactions for
     * @return a list of transaction response DTOs
     * @throws UnauthorizedAccountAccessException if the account does not belong to the customer
     */
    public List<TransactionResponse> getMyTransactionResponses(Long customerId, String accountNumber) {
        if (!isAccountOwnedByCurrentCustomer(accountNumber, customerId)) {
            throw new UnauthorizedAccountAccessException("Account does not belong to current User");
        }
        return findByFromAccountNumberOrToAccountNumber(accountNumber)
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    /**
     * Retrieves all transactions originating from an account owned by the specified customer,
     * returning them as DTOs.
     *
     * @param customerId the ID of the customer requesting their transactions
     * @param fromAccountNumber the source account number to query transactions for
     * @return a list of transaction response DTOs
     * @throws UnauthorizedAccountAccessException if the account does not belong to the customer
     */
    public List<TransactionResponse> getMyTransactionsByFromAccountResponses(Long customerId, String fromAccountNumber) {
        if (!isAccountOwnedByCurrentCustomer(fromAccountNumber, customerId)) {
            throw new UnauthorizedAccountAccessException("Account does not belong to current User");
        }
        return findTransactionsByFromAccountNumber(fromAccountNumber)
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    /**
     * Retrieves all transactions received by an account owned by the specified customer,
     * returning them as DTOs.
     *
     * @param customerId the ID of the customer requesting their transactions
     * @param toAccountNumber the destination account number to query transactions for
     * @return a list of transaction response DTOs
     * @throws UnauthorizedAccountAccessException if the account does not belong to the customer
     */
    public List<TransactionResponse> getMyTransactionsByToAccountResponses(Long customerId, String toAccountNumber) {
        if (!isAccountOwnedByCurrentCustomer(toAccountNumber, customerId)) {
            throw new UnauthorizedAccountAccessException("Account does not belong to current User");
        }
        return findTransactionsByToAccountNumber(toAccountNumber)
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    /**
     * Retrieves all transactions between two specific accounts where the source account
     * is owned by the specified customer, returning them as DTOs.
     *
     * @param customerId the ID of the customer requesting their transactions
     * @param fromAccountNumber the source account number (must be owned by the customer)
     * @param toAccountNumber the destination account number
     * @return a list of transaction response DTOs
     * @throws UnauthorizedAccountAccessException if the source account does not belong to the customer
     */
    public List<TransactionResponse> getMyTransactionsByFromAndToAccountResponses(Long customerId, String fromAccountNumber, String toAccountNumber) {
        if (!isAccountOwnedByCurrentCustomer(fromAccountNumber, customerId)) {
            throw new UnauthorizedAccountAccessException("From account does not belong to current User");
        }
        return findTransactionsByFromAndToAccountNumber(fromAccountNumber, toAccountNumber)
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    /**
     * Retrieves all transactions with a specific status for an account owned by the specified customer,
     * returning them as DTOs.
     *
     * @param customerId the ID of the customer requesting their transactions
     * @param accountNumber the account number to query transactions for
     * @param status the transaction status to filter by
     * @return a list of transaction response DTOs
     * @throws UnauthorizedAccountAccessException if the account does not belong to the customer
     */
    public List<TransactionResponse> getMyTransactionsByStatusResponses(Long customerId, String accountNumber, TransactionStatus status) {
        if (!isAccountOwnedByCurrentCustomer(accountNumber, customerId)) {
            throw new UnauthorizedAccountAccessException("Account does not belong to current User");
        }
        return findByFromAccountNumberAndStatus(accountNumber, status)
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    /**
     * Retrieves all transactions of a specific type for an account owned by the specified customer,
     * returning them as DTOs.
     *
     * @param customerId the ID of the customer requesting their transactions
     * @param accountNumber the account number to query transactions for
     * @param type the transaction type to filter by
     * @return a list of transaction response DTOs
     * @throws UnauthorizedAccountAccessException if the account does not belong to the customer
     */
    public List<TransactionResponse> getMyTransactionsByTypeResponses(Long customerId, String accountNumber, TransactionType type) {
        if (!isAccountOwnedByCurrentCustomer(accountNumber, customerId)) {
            throw new UnauthorizedAccountAccessException("Account does not belong to current User");
        }
        return findByFromAccountNumberAndType(accountNumber, type)
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    /**
     * Creates a deposit transaction and updates the account balance.
     *
     * <p>This operation is idempotent - submitting the same request with the same idempotency key
     * will return the original transaction response without creating a duplicate.</p>
     *
     * @param customerId the ID of the customer making the deposit
     * @param idempotencyKey a unique key to ensure idempotent processing
     * @param request the deposit transaction request containing account and amount details
     * @return the created transaction as a response DTO
     * @throws DepositFailedException if the account service rejects the deposit operation
     * @throws AccountServiceUnavailableException if the account service is unreachable
     */
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

    /**
     * Creates a payment transaction, transferring funds from one account to another.
     *
     * <p>This operation is idempotent - submitting the same request with the same idempotency key
     * will return the original transaction response without creating a duplicate.</p>
     *
     * @param customerId the ID of the customer initiating the payment
     * @param idempotencyKey a unique key to ensure idempotent processing
     * @param request the payment transaction request containing source, destination, and amount
     * @return the created transaction as a response DTO
     * @throws PaymentFailedException if the account service rejects the payment operation
     * @throws AccountServiceUnavailableException if the account service is unreachable
     */
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

    /**
     * Creates a refund transaction, reversing a previous transaction.
     *
     * <p>This operation reverses the original transaction by creating a new refund transaction
     * that transfers funds back from the destination to the source. The original transaction
     * is marked as REVERSED to prevent duplicate refunds.</p>
     *
     * <p>This operation is idempotent - submitting the same request with the same idempotency key
     * will return the original refund transaction response without creating a duplicate.</p>
     *
     * @param customerId the ID of the customer requesting the refund
     * @param idempotencyKey a unique key to ensure idempotent processing
     * @param request the refund transaction request containing the original transaction ID
     * @return the created refund transaction as a response DTO
     * @throws TransactionNotFoundException if the original transaction does not exist
     * @throws TransactionAlreadyRefundedException if the original transaction has already been reversed
     * @throws RefundFailedException if the account service rejects the refund operation
     * @throws AccountServiceUnavailableException if the account service is unreachable
     */
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

    /**
     * Verifies whether a given account is owned by the specified customer.
     *
     * <p>This method communicates with the account service to perform the ownership check.
     * It uses service-to-service authentication via JWT tokens.</p>
     *
     * @param accountNumber the account number to verify
     * @param customerId the customer ID claiming ownership
     * @return true if the account belongs to the customer, false otherwise
     * @throws AccountServiceUnavailableException if the account service is unreachable or returns an error
     */
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

    /**
     * Executes a deposit operation by calling the account service to update the account balance.
     *
     * @param customerId the ID of the customer making the deposit
     * @param accountNumber the account number to deposit into
     * @param amount the amount to deposit
     * @throws DepositFailedException if the account service rejects the deposit
     * @throws AccountServiceUnavailableException if the account service is unreachable
     */
    private void performDeposit(Long customerId, String accountNumber, BigDecimal amount) {
        executeAccountCall(
                transactionProperties.getEndpointDeposit(),
                new ServiceDepositBalanceRequest(customerId, accountNumber, amount),
                DepositFailedException::new
        );
    }

    /**
     * Executes a payment operation by calling the account service to transfer funds between accounts.
     *
     * @param customerId the ID of the customer initiating the payment
     * @param fromAccountNumber the source account number
     * @param toAccountNumber the destination account number
     * @param amount the amount to transfer
     * @throws PaymentFailedException if the account service rejects the payment
     * @throws AccountServiceUnavailableException if the account service is unreachable
     */
    private void performPayment(Long customerId, String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        executeAccountCall(
                transactionProperties.getEndpointPayment(),
                new ServicePaymentRequest(customerId, fromAccountNumber, toAccountNumber, amount),
                PaymentFailedException::new
        );
    }

    /**
     * Executes a refund operation by calling the account service to reverse a previous transaction.
     *
     * @param customerId the ID of the customer requesting the refund
     * @param fromAccountNumber the source account number for the refund
     * @param toAccountNumber the destination account number for the refund
     * @param amount the amount to refund
     * @throws RefundFailedException if the account service rejects the refund
     * @throws AccountServiceUnavailableException if the account service is unreachable
     */
    private void performRefund(Long customerId, String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        executeAccountCall(
                transactionProperties.getEndpointRefund(),
                new ServiceRefundBalanceRequest(customerId, toAccountNumber, fromAccountNumber, amount),
                RefundFailedException::new
        );
    }

    /**
     * Generic method to execute account service calls with standardized error handling.
     *
     * <p>This method handles the common pattern of calling the account service with a JWT token,
     * sending a request body, and mapping errors to appropriate exceptions.</p>
     *
     * @param endpoint the account service endpoint path
     * @param body the request body to send
     * @param exceptionFactory a function to create the appropriate exception type from an error message
     * @throws RuntimeException as determined by the exceptionFactory when the call fails
     */
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