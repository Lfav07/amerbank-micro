package com.amerbank.account.dto;

import com.amerbank.account.model.AccountStatus;
import com.amerbank.account.model.AccountType;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String accountNumber,
        Long customerId,
        BigDecimal balance,
        AccountType type,
        AccountStatus status
) {}