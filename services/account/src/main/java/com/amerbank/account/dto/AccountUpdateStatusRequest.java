package com.amerbank.account.dto;

import com.amerbank.account.model.AccountStatus;
import jakarta.validation.constraints.NotBlank;

public record AccountUpdateStatusRequest(
        @NotBlank
        AccountStatus status
) {
}
