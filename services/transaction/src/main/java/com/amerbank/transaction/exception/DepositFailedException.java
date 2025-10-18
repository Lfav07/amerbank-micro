package com.amerbank.transaction.exception;

public class DepositFailedException extends RuntimeException {
    public DepositFailedException(String message) {
        super(message);
    }
}
