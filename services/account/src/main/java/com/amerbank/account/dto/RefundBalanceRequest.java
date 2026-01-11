package com.amerbank.account.dto;

import java.math.BigDecimal;

public record RefundBalanceRequest(
         String fromAccountNumber,
         String toAccountNumber,
        BigDecimal amount

    ) {}


