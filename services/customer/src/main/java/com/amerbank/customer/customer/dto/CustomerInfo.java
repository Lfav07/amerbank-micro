package com.amerbank.customer.customer.dto;

import java.time.LocalDate;

public record CustomerInfo(
        String firstName,
        String lastName,
        LocalDate dateOfBirth
) {}
