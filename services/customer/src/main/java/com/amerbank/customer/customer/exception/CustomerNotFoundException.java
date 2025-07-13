package com.amerbank.customer.customer.exception;

public class CustomerNotFoundException extends  RuntimeException {

    public  CustomerNotFoundException(String message) {
        super(message);
    }
}
