package com.amerbank.account.dto.request;

import com.amerbank.account.model.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record AccountUpdateStatusRequest(
        @NotNull
        AccountStatus status
) {
}
