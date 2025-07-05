package com.amerbank.customer.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CustomerRequest(

        @NotNull(message = "User ID is required")
        Long userId,

        @NotBlank(message = "Customer first name is required")
        String firstName,

        @NotBlank(message = "Customer last name is required")
        String lastName,

        @NotNull(message = "Date of birth is required")
        LocalDate dateOfBirth
) {}
