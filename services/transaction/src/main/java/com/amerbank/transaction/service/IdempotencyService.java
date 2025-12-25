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

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final TransactionRepository transactionRepository;

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
            transactionRepository.save(transaction);
            effect.accept(transaction);
            transaction.setStatus(TransactionStatus.APPROVED);

        } catch (DataIntegrityViolationException ex) {
            return transactionRepository.findByIdempotencyKey(idempotencyKey)
                    .map(mapper)
                    .orElseThrow();

        } catch (RuntimeException ex) {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(ex.getMessage());
            throw ex;

        } finally {
            transactionRepository.save(transaction);
        }

        return mapper.apply(transaction);
    }
}
