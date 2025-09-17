package com.amerbank.auth_server.dto;

public record EmailUpdateRequest(
        String newEmail, String password

) {}
