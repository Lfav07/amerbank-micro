package com.amerbank.customer.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PasswordUpdateRequest(
        @NotNull
        Long customerId,

        @NotBlank
        String oldPassword,

        @NotBlank
        String newPassword) {
}
