package com.amerbank.loan.exception;

public class LoanNotEligibleException extends RuntimeException {
    public LoanNotEligibleException(String message) { super(message); }
}