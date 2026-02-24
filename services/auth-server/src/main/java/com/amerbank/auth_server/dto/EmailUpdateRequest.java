package com.amerbank.auth_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to update email address")
public record EmailUpdateRequest(
        @Schema(description = "New email address", example = "newemail@example.com")
        String newEmail,
        @Schema(description = "Current password for verification", example = "password123")
        String password

) {}
