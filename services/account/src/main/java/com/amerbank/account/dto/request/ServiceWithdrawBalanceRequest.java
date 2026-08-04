package com.amerbank.account.dto.request;

import java.math.BigDecimal;

public record ServiceWithdrawBalanceRequest(
        Long customerId,
        String accountNumber,
        BigDecimal amount
) {}
