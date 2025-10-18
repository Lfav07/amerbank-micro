package com.amerbank.common_dto;

import java.math.BigDecimal;
import java.util.UUID;

 public record RefundBalanceRequest(
         String fromAccountNumber,
         String toAccountNumber,
        BigDecimal amount

    ) {}


