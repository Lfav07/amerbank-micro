package com.amerbank.account.dto.request;

import java.math.BigDecimal;

public record PaymentBalanceRequest(
    String fromAccountNumber,
    String toAccountNumber,
    BigDecimal amount
    ) {}
