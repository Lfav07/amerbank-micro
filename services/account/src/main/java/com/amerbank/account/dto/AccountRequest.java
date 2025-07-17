package com.amerbank.account.dto;

import com.amerbank.account.model.AccountType;
import jakarta.validation.constraints.NotNull;

public record AccountRequest(

        @NotNull
        AccountType type
) {}
