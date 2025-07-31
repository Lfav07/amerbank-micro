package com.amerbank.transaction.service;

import com.amerbank.transaction.dto.DepositTransactionRequest;
import com.amerbank.transaction.dto.PaymentTransactionRequest;
import com.amerbank.transaction.dto.RefundTransactionRequest;
import com.amerbank.transaction.dto.TransactionResponse;
import com.amerbank.transaction.model.Transaction;
import com.amerbank.transaction.model.TransactionStatus;
import com.amerbank.transaction.model.TransactionType;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(Transaction transaction) {
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

    public Transaction toTransaction(DepositTransactionRequest request) {
        Transaction transaction = new Transaction();
        transaction.setAmount(request.amount());
        transaction.setFromAccountNumber(request.fromAccountNumber());
        transaction.setToAccountNumber(request.toAccountNumber());
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setStatus(TransactionStatus.WAITING);
        return transaction;
    }

    public Transaction toTransaction(PaymentTransactionRequest request) {
        Transaction transaction = new Transaction();
        transaction.setAmount(request.amount());
        transaction.setFromAccountNumber(request.fromAccountNumber());
        transaction.setToAccountNumber(request.toAccountNumber());
        transaction.setType(TransactionType.PAYMENT);
        transaction.setStatus(TransactionStatus.WAITING);
        return  transaction;
    }




}
