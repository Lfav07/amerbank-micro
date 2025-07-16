package com.amerbank.common_dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CustomerResponse(
        Long id,
        Long userId,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        boolean kycVerified,
        LocalDateTime createdAt
) {}
