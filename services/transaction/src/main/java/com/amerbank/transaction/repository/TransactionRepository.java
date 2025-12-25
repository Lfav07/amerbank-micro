package com.amerbank.transaction.repository;

import com.amerbank.transaction.model.Transaction;
import com.amerbank.transaction.model.TransactionStatus;
import com.amerbank.transaction.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {


    List<Transaction> findByFromAccountNumberOrToAccountNumber(String fromAccount, String toAccount);


    List<Transaction> findByFromAccountNumber(String fromAccount);

    List<Transaction> findByFromAccountNumberAndToAccountNumber(String fromAccount, String toAccount);


    List<Transaction> findByToAccountNumber(String toAccount);


    List<Transaction> findByStatus(TransactionStatus status);

    List<Transaction> findByType(TransactionType type);


    List<Transaction> findByFromAccountNumberAndStatusOrderByCreatedAtDesc(String fromAccount, TransactionStatus status);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    List<Transaction> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);


    long countByType(TransactionType type);


    List<Transaction> findByFromAccountNumberAndStatus(String fromAccount, TransactionStatus status);

    List<Transaction> findByFromAccountNumberAndType(String fromAccount, TransactionType type);


    Optional<Transaction> findByIdAndFromAccountNumber(UUID id, String fromAccount);


    List<Transaction> findTop5ByFromAccountNumberOrderByCreatedAtDesc(String fromAccount);
}
