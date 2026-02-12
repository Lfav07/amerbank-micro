package com.amerbank.auth_server.dto;

import jakarta.validation.constraints.NotNull;

public record CustomerRegistrationResponse(
        @NotNull Long id
)
{}
