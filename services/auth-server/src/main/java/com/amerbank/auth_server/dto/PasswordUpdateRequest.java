package com.amerbank.auth_server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PasswordUpdateRequest(
        @NotNull
        Long userId,

        @NotBlank
        String oldPassword,

        @NotBlank
        String newPassword) {
}
