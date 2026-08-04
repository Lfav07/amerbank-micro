package com.amerbank.loan.exception;

public class LoanRepaymentFailedException extends RuntimeException {
    public LoanRepaymentFailedException(String message) { super(message); }
}