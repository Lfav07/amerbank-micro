package com.amerbank.loan.exception;

public class AccountServiceUnavailableException extends RuntimeException {
    public AccountServiceUnavailableException(String message) { super(message); }
}