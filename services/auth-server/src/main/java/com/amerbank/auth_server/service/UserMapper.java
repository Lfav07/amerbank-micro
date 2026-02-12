package com.amerbank.auth_server.service;
import com.amerbank.auth_server.dto.UserResponse;
import com.amerbank.auth_server.dto.UserResponseCustom;
import com.amerbank.auth_server.model.User;
import org.springframework.stereotype.Service;

@Service
public class UserMapper {

    public UserResponse toResponse(User user) {
        return  new UserResponse(user.getId(), user.getCustomerId(), user.getEmail());
    }

}
