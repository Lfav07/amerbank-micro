package com.amerbank.auth_server.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UserLoginRequest(
     @NotBlank String email,
     @NotBlank String password
) {}