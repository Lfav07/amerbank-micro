package com.amerbank.auth_server.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Admin registration request")
public record AdminRegisterRequest(
        @Schema(description = "Admin email address", example = "admin@amerbank.com")
        @Email String email,
        @Schema(description = "Admin password (minimum 4 characters)", example = "admin123")
        @NotBlank  @Size(min = 4, message = "Password too short")  String password
) {}
