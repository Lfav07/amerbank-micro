package com.amerbank.customer.customer.exception;

public class CustomerRegistrationFailedException extends RuntimeException {
    public CustomerRegistrationFailedException(String message) {
        super(message);
    }
}
