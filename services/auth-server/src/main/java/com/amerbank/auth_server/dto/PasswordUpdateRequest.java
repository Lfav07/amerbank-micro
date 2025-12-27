package com.amerbank.auth_server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PasswordUpdateRequest(
        @NotBlank
        String currentPassword,

        @NotBlank
        String newPassword) {
}
