package com.amerbank.customer.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CustomerRequest(

        @NotBlank(message = "Customer first name is required")
        String firstName,

        @NotBlank(message = "Customer last name is required")
        String lastName,

        @NotBlank(message = "Password is required")
        String password,

        @NotBlank(message = "Customer Email is required")
        @Email(message = "Customer Email is not a valid email address")
        String email,

        @NotNull(message = "Date of birth is required")
        LocalDate dateOfBirth

) {
}
