package com.amerbank.customer.customer.dto;

import java.time.LocalDate;

public record CustomerResponse(
        Long id,

        String firstName,

        String lastName,


        LocalDate dateOfBirth,

        boolean kycVerified) {
}
