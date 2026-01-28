package com.amerbank.account.exception;

public class NullAccountBalanceException extends RuntimeException {
    public NullAccountBalanceException(String message) {
        super(message);
    }
}
