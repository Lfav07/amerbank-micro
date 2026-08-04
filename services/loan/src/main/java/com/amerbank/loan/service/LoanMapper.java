package com.amerbank.loan.service;

import com.amerbank.loan.dto.LoanInfo;
import com.amerbank.loan.dto.response.LoanPaymentResponse;
import com.amerbank.loan.dto.response.LoanResponse;
import com.amerbank.loan.model.Loan;
import com.amerbank.loan.model.LoanPayment;
import org.springframework.stereotype.Component;

@Component
public class LoanMapper {

    public LoanResponse toResponse(Loan loan) {
        return new LoanResponse(
                loan.getId(),
                loan.getLoanNumber(),
                loan.getCustomerId(),
                loan.getAccountNumber(),
                loan.getPrincipalAmount(),
                loan.getInterestRate(),
                loan.getTermMonths(),
                loan.getMonthlyPayment(),
                loan.getTotalAmount(),
                loan.getRemainingBalance(),
                loan.getType(),
                loan.getStatus(),
                loan.getDisbursedAt(),
                loan.getMaturityDate()
        );
    }

    public LoanInfo toLoanInfo(Loan loan) {
        return new LoanInfo(
                loan.getId(),
                loan.getLoanNumber(),
                loan.getPrincipalAmount(),
                loan.getRemainingBalance(),
                loan.getType(),
                loan.getStatus(),
                loan.getMonthlyPayment()
        );
    }

    public LoanPaymentResponse toPaymentResponse(LoanPayment payment) {
        return new LoanPaymentResponse(
                payment.getId(),
                payment.getPaymentNumber(),
                payment.getAmountDue(),
                payment.getPrincipalPortion(),
                payment.getInterestPortion(),
                payment.getDueDate(),
                payment.getPaidDate(),
                payment.getStatus()
        );
    }
}