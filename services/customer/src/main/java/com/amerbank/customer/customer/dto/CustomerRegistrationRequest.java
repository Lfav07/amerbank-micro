package com.amerbank.customer.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Customer registration request (internal)")
public record CustomerRegistrationRequest(
        @Schema(description = "Customer first name", example = "John")
        @NotBlank(message = "First name is required")
        String firstName,

        @Schema(description = "Customer last name", example = "Doe")
        @NotBlank(message = "Last name is required")
        String lastName,

        @Schema(description = "Associated user ID", example = "1")
        @NotNull
        Long userId,

        @Schema(description = "Customer date of birth", example = "1990-01-15")
        @NotNull(message = "Date of birth is required")
        LocalDate dateOfBirth
) {}
