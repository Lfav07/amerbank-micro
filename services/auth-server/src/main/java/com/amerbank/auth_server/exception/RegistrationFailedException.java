package com.amerbank.auth_server.exception;

public class RegistrationFailedException extends RuntimeException {
    public RegistrationFailedException(String message, RuntimeException ex) {
        super(message);
    }
}
