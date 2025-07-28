package com.amerbank.common_dto;

import java.math.BigDecimal;

public record UpdateBalanceRequest(
        String accountNumber,
        BigDecimal amount
) {}