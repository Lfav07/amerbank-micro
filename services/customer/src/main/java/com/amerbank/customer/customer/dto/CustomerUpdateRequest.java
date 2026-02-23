package com.amerbank.customer.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Customer update request")
public record CustomerUpdateRequest(
        @Schema(description = "Customer first name", example = "John")
        @NotBlank(message = "First name is required")
        String firstName,

        @Schema(description = "Customer last name", example = "Doe")
        @NotBlank(message = "Last name is required")
        String lastName
) {}
