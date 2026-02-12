package com.amerbank.auth_server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserResponse(

       @NotNull Long id,

       @NotNull Long customerId,

       @NotBlank String email
) {}