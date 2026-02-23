package com.amerbank.auth_server.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "User registration request")
public record UserRegisterRequest(
        @Schema(description = "User email address", example = "john.doe@example.com")
        @Email
        String email,
        @Schema(description = "User password (minimum 4 characters)", example = "password123")
        @NotBlank @Size(min = 4, message = "Password too short")
        String password,

        @Schema(description = "User first name", example = "John")
        @NotBlank(message = "First name is required")
        String firstName,

        @Schema(description = "User last name", example = "Doe")
        @NotBlank(message = "Last name is required")
        String lastName,

        @Schema(description = "User date of birth", example = "1990-01-15")
        @NotNull(message = "Date of birth is required")
        LocalDate dateOfBirth
) {
}
