package com.amerbank.auth_server.service;

import com.amerbank.auth_server.model.User;
import com.amerbank.common_dto.UserResponse;

public class UserMapper {

    public UserResponse toResponse(User user) {
        return  new UserResponse(user.getId(),user.getEmail());
    }
}
