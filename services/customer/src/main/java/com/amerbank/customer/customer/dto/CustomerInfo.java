package com.amerbank.customer.customer.dto;

import java.time.LocalDate;

public record CustomerInfo(
        Long id,
        String firstName,
        String lastName,
        LocalDate dateOfBirth
) {}
