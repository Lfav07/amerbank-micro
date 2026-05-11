package com.amerbank.account.dto.request;

import java.math.BigDecimal;

public record RefundBalanceRequest(
         String fromAccountNumber,
         String toAccountNumber,
        BigDecimal amount

    ) {}


