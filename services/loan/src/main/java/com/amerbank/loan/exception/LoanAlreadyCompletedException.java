package com.amerbank.loan.exception;

public class LoanAlreadyCompletedException extends RuntimeException {
    public LoanAlreadyCompletedException(String message) { super(message); }
}