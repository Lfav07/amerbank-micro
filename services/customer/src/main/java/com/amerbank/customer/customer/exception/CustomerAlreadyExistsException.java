package com.amerbank.customer.customer.exception;

public class CustomerAlreadyExistsException extends RuntimeException {
    public CustomerAlreadyExistsException(String s) {
        super(s);
    }
}
