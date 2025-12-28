package com.amerbank.auth_server.controller;

import com.amerbank.auth_server.dto.*;
import com.amerbank.auth_server.model.User;
import com.amerbank.auth_server.security.JwtUserPrincipal;
import com.amerbank.auth_server.service.UserMapper;
import com.amerbank.auth_server.service.UserService;
import com.amerbank.common_dto.AuthenticationResponse;
import com.amerbank.common_dto.UserLoginRequest;
import com.amerbank.common_dto.UserRegisterRequest;
import com.amerbank.common_dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper mapper;

    // -------------------- Helper --------------------
    private Map<String, String> message(String msg) {
        return Map.of("message", msg);
    }

    // -------------------- Public Endpoints --------------------
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRegisterRequest request) {
       UserResponse response =  userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody UserLoginRequest request) {
        AuthenticationResponse response = userService.login(request);
        return ResponseEntity.ok(Map.of("token", response.token()));
    }

    @PatchMapping("/me/email")
    public ResponseEntity<Void> updateEmail(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Valid @RequestBody EmailUpdateRequest request) {


        userService.updateEmail(principal.userId(), request.newEmail());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Map<String, String>> updatePassword(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Valid @RequestBody PasswordUpdateRequest request) {

            userService.updatePassword(principal.userId(), request);
            return ResponseEntity.ok(message("Password successfully updated"));
        }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyUserInfo(@AuthenticationPrincipal JwtUserPrincipal principal) {
       UserResponse response = userService.getOwnUserInfo(principal.userId());
        return ResponseEntity.ok(response);
    }

    // -------------------- Admin / Management Endpoints --------------------
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/users")
    public ResponseEntity<Void> deleteAllUsers() {
        userService.deleteAllUsers();
        return ResponseEntity.noContent().build();
    }





    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(mapper.toResponse(user));
    }

    @GetMapping("/users/by-email")
    public ResponseEntity<UserResponse> getUserByEmail(@RequestParam String email) {
        User user = userService.findByEmail(email);
        return ResponseEntity.ok(mapper.toResponse(user));
    }
}
