package com.amerbank.loan.exception;

public class LoanNotDisbursedException extends RuntimeException {
    public LoanNotDisbursedException(String message) { super(message); }
}