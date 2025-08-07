package com.amerbank.transaction.exception;

public class TransactionAlreadyRefundedException extends RuntimeException {
    public TransactionAlreadyRefundedException(String message) {
        super(message);
    }
}
