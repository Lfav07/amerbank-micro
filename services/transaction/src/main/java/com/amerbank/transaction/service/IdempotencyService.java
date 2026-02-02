package com.amerbank.transaction.service;

import com.amerbank.transaction.model.Transaction;
import com.amerbank.transaction.model.TransactionStatus;
import com.amerbank.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Service that ensures transaction operations are idempotent, preventing duplicate processing
 * when the same request is submitted multiple times.
 *
 * <p>Idempotency guarantees that executing the same operation multiple times with the same
 * idempotency key produces the same result as executing it once. This is critical for
 * financial transactions where network failures or retries could otherwise create duplicates.</p>
 *
 * <p>The service uses a database constraint on the idempotency key to ensure atomicity.
 * If a duplicate request arrives, it returns the result of the original transaction instead
 * of creating a new one.</p>
 */
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final TransactionRepository transactionRepository;

    /**
     * Executes a transaction operation idempotently based on the provided idempotency key.
     *
     * <p>If a transaction with the same idempotency key already exists, returns the existing
     * transaction without re-executing the operation. Otherwise, creates a new transaction,
     * executes the associated side effect (like updating account balances), and returns the result.</p>
     *
     * <p><strong>Flow:</strong></p>
     * <ol>
     *   <li>Check if a transaction with this idempotency key exists</li>
     *   <li>If yes: return the existing transaction mapped to the response type</li>
     *   <li>If no: create new transaction, execute side effect, and return the result</li>
     * </ol>
     *
     * @param <R> the return type after mapping the transaction
     * @param idempotencyKey unique key to identify this operation (prevents duplicates)
     * @param creator supplier that creates the initial transaction entity
     * @param effect consumer that executes side effects (e.g., calling account service)
     * @param mapper function that converts the transaction to the desired response type
     * @return the mapped result from either the existing or newly created transaction
     */
    public <R> R execute(
            String idempotencyKey,
            Supplier<Transaction> creator,
            Consumer<Transaction> effect,
            Function<Transaction, R> mapper
    ) {
        return transactionRepository.findByIdempotencyKey(idempotencyKey)
                .map(mapper)
                .orElseGet(() -> createAndExecute(idempotencyKey, creator, effect, mapper));
    }

    /**
     * Creates and executes a new transaction with proper status tracking and error handling.
     *
     * <p><strong>Execution Flow:</strong></p>
     * <ol>
     *   <li>Create transaction entity with WAITING status</li>
     *   <li>Save to database (establishes idempotency key constraint)</li>
     *   <li>Execute the side effect (e.g., deposit, payment, refund)</li>
     *   <li>Update status to APPROVED on success</li>
     *   <li>Save final status to database</li>
     * </ol>
     *
     * <p><strong>Race Condition Handling:</strong></p>
     * <p>If two identical requests arrive simultaneously, both will attempt to save the transaction.
     * The database unique constraint on idempotency_key will cause one to fail with a
     * {@link DataIntegrityViolationException}. The losing thread catches this exception,
     * retrieves the transaction created by the winning thread, and returns it - ensuring
     * the side effect only executes once.</p>
     *
     * <p><strong>Failure Handling:</strong></p>
     * <p>If the side effect fails (e.g., insufficient funds), the transaction status is set to
     * FAILED with the error message stored for debugging. The exception is re-thrown to notify
     * the caller, but the failed transaction remains in the database to maintain idempotency.</p>
     *
     * @param <R> the return type after mapping the transaction
     * @param idempotencyKey unique key for this operation
     * @param creator supplier that creates the transaction entity
     * @param effect consumer that performs the actual operation (account balance update)
     * @param mapper function to convert transaction to response type
     * @return the mapped transaction result
     * @throws RuntimeException if the side effect fails (re-thrown after marking transaction as FAILED)
     */
    private <R> R createAndExecute(
            String idempotencyKey,
            Supplier<Transaction> creator,
            Consumer<Transaction> effect,
            Function<Transaction, R> mapper
    ) {

        Transaction transaction = creator.get();
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setStatus(TransactionStatus.WAITING);

        try {
            // Save transaction first to claim the idempotency key
            transactionRepository.save(transaction);

            // Execute the actual operation (deposit/payment/refund)
            effect.accept(transaction);

            // Mark as approved if successful
            transaction.setStatus(TransactionStatus.APPROVED);

        } catch (DataIntegrityViolationException ex) {
            // Race condition: another thread already created this transaction
            // Retrieve and return the existing transaction instead
            return transactionRepository.findByIdempotencyKey(idempotencyKey)
                    .map(mapper)
                    .orElseThrow();

        } catch (RuntimeException ex) {
            // Operation failed (e.g., insufficient funds, account service error)
            // Mark transaction as failed and preserve error message
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(ex.getMessage());
            throw ex;

        } finally {
            // Always save final transaction state (APPROVED or FAILED)
            transactionRepository.save(transaction);
        }

        return mapper.apply(transaction);
    }
}