package com.amerbank.account.dto;

import com.amerbank.account.model.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Account creation request")
public record AccountRequest(

        @Schema(description = "Type of account to create", example = "CHECKING")
        @NotNull
        AccountType type
) {}
