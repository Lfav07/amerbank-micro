package com.amerbank.account.dto;

import com.amerbank.account.model.AccountType;
import jakarta.validation.constraints.NotBlank;

public record AccountUpdateTypeRequest(
        @NotBlank
        String accountNumber,
        AccountType type
) {
}
