package com.amerbank.customer.customer.dto;

import jakarta.validation.constraints.NotNull;

public record CustomerRegistrationResponse(
        @NotNull Long id
)
{}
