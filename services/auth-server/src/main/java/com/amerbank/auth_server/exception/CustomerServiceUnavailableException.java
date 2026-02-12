package com.amerbank.auth_server.exception;

public class CustomerServiceUnavailableException extends RuntimeException {
    public CustomerServiceUnavailableException(String message) {
        super(message);
    }
}
