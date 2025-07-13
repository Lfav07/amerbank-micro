package com.amerbank.auth_server.controller;

import com.amerbank.auth_server.model.User;
import com.amerbank.auth_server.service.UserService;
import com.amerbank.common_dto.UserRegisterRequest;
import com.amerbank.common_dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody UserRegisterRequest request) {
        User user = userService.registerAdmin(request);

        UserResponse response = new UserResponse(user.getId(), user.getEmail());
        return ResponseEntity.ok(response);
    }

}
