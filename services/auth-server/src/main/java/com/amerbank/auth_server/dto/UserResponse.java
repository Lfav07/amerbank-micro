package com.amerbank.auth_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "User response")
public record UserResponse(

       @Schema(description = "User unique identifier", example = "1")
       @NotNull Long id,

       @Schema(description = "Associated customer identifier", example = "100")
       @NotNull Long customerId,

       @Schema(description = "User email address", example = "john.doe@example.com")
       @NotBlank String email
) {}
