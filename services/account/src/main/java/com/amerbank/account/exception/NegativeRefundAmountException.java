package com.amerbank.account.exception;

public class NegativeRefundAmountException extends RuntimeException {
    public NegativeRefundAmountException(String message) {
        super(message);
    }
}
