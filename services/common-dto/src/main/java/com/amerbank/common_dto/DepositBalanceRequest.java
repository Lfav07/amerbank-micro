package com.amerbank.common_dto;

import java.math.BigDecimal;

public record DepositBalanceRequest(
        String accountNumber,
        BigDecimal amount
) {}