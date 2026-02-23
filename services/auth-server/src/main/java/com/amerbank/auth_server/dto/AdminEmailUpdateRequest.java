package com.amerbank.auth_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Admin request to update user email")
public record AdminEmailUpdateRequest(
        @Schema(description = "New email address", example = "newemail@example.com")
        @NotBlank @Email String newEmail) {
}
