package com.amerbank.loan.exception;

public class InsufficientRepaymentAmountException extends RuntimeException {
    public InsufficientRepaymentAmountException(String message) { super(message); }
}