package com.amerbank.account.exception;

public class UnauthorizedMicroserviceAccessException extends RuntimeException {
    public UnauthorizedMicroserviceAccessException(String message) {
        super(message);
    }
}
