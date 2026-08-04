package com.amerbank.loan.exception;

public class LoanNotFoundException extends RuntimeException {
    public LoanNotFoundException(String message) { super(message); }
}