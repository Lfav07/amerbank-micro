package com.amerbank.customer.customer.dto.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRegisterRequest(
        @Email String email,
        @NotBlank  @Size(min = 4, message = "Password too short")  String password
) {}
