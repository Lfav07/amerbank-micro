package com.amerbank.common_dto;

import java.math.BigDecimal;

public record PaymentBalanceRequest(
    String fromAccountNumber,
    String toAccountNumber,
    BigDecimal amount
    ) {}
