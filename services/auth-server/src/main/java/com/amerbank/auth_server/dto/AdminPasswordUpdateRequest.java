package com.amerbank.auth_server.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminPasswordUpdateRequest(
    @NotBlank
    String newPassword
){}
