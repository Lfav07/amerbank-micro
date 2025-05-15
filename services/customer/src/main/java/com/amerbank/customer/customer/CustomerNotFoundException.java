package com.amerbank.customer.customer;

public class CustomerNotFoundException extends  RuntimeException {

    public  CustomerNotFoundException(String message) {
        super(message);
    }
}
