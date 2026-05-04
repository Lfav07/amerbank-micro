package com.amerbank.account.dto.request;

import java.math.BigDecimal;

public record DepositBalanceRequest(
        String accountNumber,
        BigDecimal amount
) {}