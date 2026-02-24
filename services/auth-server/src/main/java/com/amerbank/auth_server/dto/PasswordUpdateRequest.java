package com.amerbank.auth_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to update user password")
public record PasswordUpdateRequest(
        @Schema(description = "Current password for verification", example = "oldpassword123")
        @NotBlank
        String currentPassword,

        @Schema(description = "New password (minimum 4 characters)", example = "newpassword123")
        @NotBlank
        String newPassword) {
}
