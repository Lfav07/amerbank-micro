package com.amerbank.customer.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CustomerResponse(
        Long id,

        String firstName,

        String lastName,

        String password,

        String email,

        LocalDate dateOfBirth,

        boolean kycVerified
) {
}
