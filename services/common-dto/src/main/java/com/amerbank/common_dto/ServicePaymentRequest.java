package com.amerbank.common_dto;

import java.math.BigDecimal;

public record ServicePaymentRequest(
        Long customerId,
        String fromAccountNumber,
        String toAccountNumber,
        BigDecimal amount
) {}