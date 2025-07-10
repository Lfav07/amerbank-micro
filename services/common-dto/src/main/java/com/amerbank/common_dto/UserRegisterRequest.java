package com.amerbank.common_dto;

import  com.amerbank.common_dto.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;


import java.util.Set;

public record UserRegisterRequest(
        @Email String email,
        @NotBlank String password,
        Set<Role> roles
) {}
