package com.amerbank.customer.customer.dto.response;

import jakarta.validation.constraints.NotNull;

public record CustomerRegistrationResponse(
        @NotNull Long id
)
{}
