package com.amerbank.account.exception;

public class InsufficientFundsAvailableException extends RuntimeException {
    public InsufficientFundsAvailableException(String message) {
        super(message);
    }
}
