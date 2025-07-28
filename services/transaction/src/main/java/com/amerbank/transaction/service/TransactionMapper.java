package com.amerbank.transaction.service;

import com.amerbank.transaction.dto.DepositTransactionRequest;
import com.amerbank.transaction.dto.TransactionResponse;
import com.amerbank.transaction.model.Transaction;
import com.amerbank.transaction.model.TransactionStatus;
import com.amerbank.transaction.model.TransactionType;

public class TransactionMapper {


    public Transaction toTransaction(DepositTransactionRequest request) {
        Transaction transaction = new Transaction();
        transaction.setAmount(request.amount());
        transaction.setFromAccountNumber(request.fromAccountNumber());
        transaction.setToAccountNumber(transaction.getToAccountNumber());
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setStatus(TransactionStatus.WAITING);
        return  transaction;
    }


    public static TransactionResponse toResponse(Transaction transaction) {
        return  new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getFromAccountNumber(),
                transaction.getToAccountNumber(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getCreatedAt()
        );
    }
}
