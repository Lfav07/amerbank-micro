package com.amerbank.auth_server.dto;

import com.amerbank.auth_server.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;


import java.util.Set;

public record UserRegisterRequest(
        @Email String email,
        @NotBlank String password,
        Set<Role> roles
) {}
