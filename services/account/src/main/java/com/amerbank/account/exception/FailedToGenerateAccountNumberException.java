package com.amerbank.account.exception;

public class FailedToGenerateAccountNumberException extends RuntimeException {
    public FailedToGenerateAccountNumberException(String message) {
        super(message);
    }
}
