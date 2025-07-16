package com.amerbank.account.dto;

import com.amerbank.account.model.AccountStatus;
import com.amerbank.account.model.AccountType;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountInfo(
        UUID id,
        String accountNumber,
        BigDecimal balance,
        AccountType type,
        AccountStatus status
) {}
