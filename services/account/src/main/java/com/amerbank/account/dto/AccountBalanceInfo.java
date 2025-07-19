package com.amerbank.account.dto;

import com.amerbank.account.model.AccountType;
import java.math.BigDecimal;

public record AccountBalanceInfo(
        String accountNumber,
        AccountType type,
        BigDecimal balance
) {}
