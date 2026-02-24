package com.amerbank.customer.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Customer response")
public record CustomerResponse(
        @Schema(description = "Customer unique identifier", example = "1")
        Long id,
        @Schema(description = "Associated user ID", example = "100")
        Long userId,
        @Schema(description = "Customer first name", example = "John")
        String firstName,
        @Schema(description = "Customer last name", example = "Doe")
        String lastName,
        @Schema(description = "Customer date of birth", example = "1990-01-15")
        LocalDate dateOfBirth,
        @Schema(description = "KYC verification status", example = "true")
        boolean kycVerified,
        @Schema(description = "Customer creation timestamp")
        LocalDateTime createdAt
) {}
