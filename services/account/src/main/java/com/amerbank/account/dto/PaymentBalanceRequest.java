package com.amerbank.account.dto;

import java.math.BigDecimal;

public record PaymentBalanceRequest(
    String fromAccountNumber,
    String toAccountNumber,
    BigDecimal amount
    ) {}
