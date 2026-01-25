package com.amerbank.account.exception;

public class AccountRegistrationFailedException extends RuntimeException {


    public AccountRegistrationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}

