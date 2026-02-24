package com.amerbank.customer.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Customer info response")
public record CustomerInfo(
        @Schema(description = "Customer unique identifier", example = "1")
        Long id,
        @Schema(description = "Customer first name", example = "John")
        String firstName,
        @Schema(description = "Customer last name", example = "Doe")
        String lastName,
        @Schema(description = "Customer date of birth", example = "1990-01-15")
        LocalDate dateOfBirth
) {}
