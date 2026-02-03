package com.amerbank.auth_server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AdminEmailUpdateRequest(
        @NotBlank @Email String newEmail) {
}
