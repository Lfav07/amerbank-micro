package com.amerbank.auth_server.dto;

public record UserLoginRequest(
        String email,
        String password
) {}