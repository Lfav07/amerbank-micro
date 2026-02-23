package com.amerbank.transaction.service;

import com.amerbank.transaction.dto.*;
import com.amerbank.transaction.exception.*;
import com.amerbank.transaction.model.Transaction;
import com.amerbank.transaction.model.TransactionStatus;
import com.amerbank.transaction.model.TransactionType;
import com.amerbank.transaction.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

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
    private final TransactionMapper transactionMapper;
    private final AccountServiceClient accountServiceClient;
    private final IdempotencyService idempotencyService;

    public TransactionService(
            TransactionRepository transactionRepository,
            TransactionMapper transactionMapper,
            AccountServiceClient accountServiceClient,
            IdempotencyService idempotencyService
    ) {
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        this.accountServiceClient = accountServiceClient;
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

    /**
     * Retrieves a transaction by its unique identifier and returns it as a DTO.
     *
     * @param id the unique identifier of the transaction
     * @return the transaction as a response DTO
     * @throws TransactionNotFoundException if no transaction exists with the given ID
     */
    public TransactionResponse getTransactionResponseById(UUID id) {
        return transactionMapper.toResponse(findTransactionById(id));
    }

    /**
     * Retrieves all transactions from a specific source account and returns them as DTOs.
     *
     * @param fromAccount the source account number
     * @return a list of transaction response DTOs
     */
    public List<TransactionResponse> getTransactionResponsesByFromAccountNumber(String fromAccount) {
        return findTransactionsByFromAccountNumber(fromAccount)
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    /**
     * Retrieves all transactions to a specific destination account and returns them as DTOs.
     *
     * @param toAccount the destination account number
     * @return a list of transaction response DTOs
     */
    public List<TransactionResponse> getTransactionResponsesByToAccountNumber(String toAccount) {
        return findTransactionsByToAccountNumber(toAccount)
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    /**
     * Retrieves all transactions between two specific accounts and returns them as DTOs.
     *
     * @param fromAccount the source account number
     * @param toAccount the destination account number
     * @return a list of transaction response DTOs
     */
    public List<TransactionResponse> getTransactionResponsesByFromAndToAccountNumber(String fromAccount, String toAccount) {
        return findTransactionsByFromAndToAccountNumber(fromAccount, toAccount)
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    /**
     * Retrieves all transactions with a specific status and returns them as DTOs.
     *
     * @param status the transaction status to filter by
     * @return a list of transaction response DTOs
     */
    public List<TransactionResponse> getTransactionResponsesByStatus(TransactionStatus status) {
        return findTransactionsByStatus(status)
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    /**
     * Retrieves all transactions of a specific type and returns them as DTOs.
     *
     * @param type the transaction type to filter by
     * @return a list of transaction response DTOs
     */
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

        if (!accountServiceClient.isAccountOwned(customerId, accountNumber)) {
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
        if (!accountServiceClient.isAccountOwned(customerId, fromAccountNumber)) {
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
        if (!accountServiceClient.isAccountOwned(customerId, toAccountNumber)) {
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
        if (!accountServiceClient.isAccountOwned(customerId, fromAccountNumber)) {
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
        if (!accountServiceClient.isAccountOwned(customerId, accountNumber)) {
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
        if (!accountServiceClient.isAccountOwned(customerId, accountNumber)) {
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
        if (!accountServiceClient.isAccountOwned(customerId, accountNumber)) {
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
        if (!accountServiceClient.isAccountOwned(customerId, fromAccountNumber)) {
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
        if (!accountServiceClient.isAccountOwned(customerId, toAccountNumber)) {
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
        if (!accountServiceClient.isAccountOwned(customerId, fromAccountNumber)) {
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
        if (!accountServiceClient.isAccountOwned(customerId, accountNumber)) {
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
        if (!accountServiceClient.isAccountOwned(customerId, accountNumber)) {
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
                tx -> accountServiceClient.deposit(customerId, request.toAccountNumber(), request.amount()),
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
                tx -> accountServiceClient.payment(
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
                    System.out.println(tx.getFromAccountNumber());
                    System.out.println(tx.getToAccountNumber());
                    System.out.println(tx.getAmount());
                    accountServiceClient.refund(
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
}