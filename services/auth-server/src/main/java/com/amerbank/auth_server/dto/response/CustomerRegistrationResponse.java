package com.amerbank.auth_server.dto.response;

import jakarta.validation.constraints.NotNull;

public record CustomerRegistrationResponse(
        @NotNull Long id
)
{}
