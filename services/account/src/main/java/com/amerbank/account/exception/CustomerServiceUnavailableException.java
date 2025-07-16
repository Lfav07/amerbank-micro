package com.amerbank.account.exception;

public class CustomerServiceUnavailableException extends RuntimeException {
    public CustomerServiceUnavailableException(String message) {
        super(message);
    }
}
