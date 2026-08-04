package com.amerbank.loan.exception;

public class LoanAlreadyDisbursedException extends RuntimeException {
    public LoanAlreadyDisbursedException(String message) { super(message); }
}