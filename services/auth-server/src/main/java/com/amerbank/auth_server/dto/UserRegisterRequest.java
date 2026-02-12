package com.amerbank.auth_server.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UserRegisterRequest(
        @Email String email,
        @NotBlank @Size(min = 4, message = "Password too short") String password,

        @NotBlank(message = "First name is required")
        String firstName,
-
        @NotBlank(message = "Last name is required")
        String lastName,

        @NotNull(message = "Date of birth is required")
        LocalDate dateOfBirth
) {
}
