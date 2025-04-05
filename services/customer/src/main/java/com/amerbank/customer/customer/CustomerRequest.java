package com.amerbank.customer.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CustomerRequest(

        @NotNull(message = "Customer first name is required")
        String firstName,

        @NotNull(message = "Customer last name is required")
        String lastName,

        @NotNull(message = "Password is required")
        String password,

        @NotNull(message = "Customer Email is required")
        @Email(message = "Customer Email is not a valid email address")
        String email,

        @NotNull(message = "Date of birth is required")
        LocalDate dateOfBirth,

        boolean kycVerified
) {
}
