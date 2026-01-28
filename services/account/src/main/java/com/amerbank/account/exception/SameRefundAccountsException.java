package com.amerbank.account.exception;

public class SameRefundAccountsException extends RuntimeException {
    public SameRefundAccountsException(String message) {
        super(message);
    }
}
