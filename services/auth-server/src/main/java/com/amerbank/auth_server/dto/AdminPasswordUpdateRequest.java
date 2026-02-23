package com.amerbank.auth_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Admin request to update user password")
public record AdminPasswordUpdateRequest(
    @Schema(description = "New password (minimum 4 characters)", example = "newpassword123")
    @NotBlank
    String newPassword
){}
