package com.amerbank.account.dto.request;

import com.amerbank.account.model.AccountType;
import jakarta.validation.constraints.NotNull;

public record AccountUpdateTypeRequest(
        @NotNull
        AccountType type
) {
}
