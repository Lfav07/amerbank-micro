package com.amerbank.auth_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "User login request")
public record UserLoginRequest(
        @Schema(description = "User email address", example = "john.doe@example.com")
        @NotBlank String email,
        @Schema(description = "User password", example = "password123")
        @NotBlank String password
) {}