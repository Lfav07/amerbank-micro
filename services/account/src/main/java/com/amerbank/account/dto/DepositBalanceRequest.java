package com.amerbank.account.dto;

import java.math.BigDecimal;

public record DepositBalanceRequest(
        String accountNumber,
        BigDecimal amount
) {}