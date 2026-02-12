package com.amerbank.auth_server.controller;

import com.amerbank.auth_server.dto.UserRegisterRequest;
import com.amerbank.auth_server.dto.UserResponse;
import com.amerbank.auth_server.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/internal")
public class InternalUserController {
    private  final UserService userService;


    @GetMapping("/users/by-email")
    public ResponseEntity<UserResponse> InternalGetUserByEmail(
            @RequestParam @Email String email) {

        return ResponseEntity.ok(userService.findByEmailMapped(email));
    }



}
