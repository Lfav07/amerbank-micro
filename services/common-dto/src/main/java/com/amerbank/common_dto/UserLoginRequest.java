package com.amerbank.common_dto;

public record UserLoginRequest(
        String email,
        String password
) {}