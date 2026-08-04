package com.amerbank.loan.exception;

public class LoanPaymentOverdueException extends RuntimeException {
    public LoanPaymentOverdueException(String message) { super(message); }
}