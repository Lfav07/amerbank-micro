package com.amerbank.account.dto;

import com.amerbank.account.model.AccountStatus;
import com.amerbank.account.model.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AccountRequest(

        @NotNull
        AccountType type,

        @NotNull
        AccountStatus status
) {}
