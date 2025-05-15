package com.amerbank.customer.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CustomerUpdateRequest(
    @NotNull
    Long customerId,

    @NotBlank
    String firstName,

    @NotBlank
    String lastname,

    @NotBlank
    @Email
    String email
){
}
