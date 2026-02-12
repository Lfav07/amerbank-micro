package com.amerbank.auth_server.exception;

public class CustomerRegistrationFailedException extends RuntimeException {
    public CustomerRegistrationFailedException(String message) {
        super(message);
    }
}
