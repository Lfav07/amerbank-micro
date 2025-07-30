package com.amerbank.common_dto;

import java.math.BigDecimal;
import java.util.UUID;

 public record RefundBalanceRequest(
        BigDecimal amount,
         String fromAccountNumber,
         String toAccountNumber

    ) {}


