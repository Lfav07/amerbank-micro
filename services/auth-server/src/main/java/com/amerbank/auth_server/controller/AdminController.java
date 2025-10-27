package com.amerbank.auth_server.controller;

import com.amerbank.auth_server.dto.AdminEmailUpdateRequest;
import com.amerbank.auth_server.dto.AdminPasswordUpdateRequest;
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
@RequestMapping("/auth/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final UserMapper mapper;

    // -------------------- Helper --------------------
    private Map<String, String> message(String msg) {
        return Map.of("message", msg);
    }

    // -------------------- Admin Authentication --------------------
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody UserRegisterRequest request) {
        userService.registerAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(message("Admin successfully registered."));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody UserLoginRequest request) {
        AuthenticationResponse response = userService.loginAdmin(request);
        return ResponseEntity.ok(Map.of("token", response.token()));
    }

    // -------------------- User Management Endpoints --------------------

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers()
                .stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(mapper.toResponse(user));
    }

    @GetMapping("/users/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        User user = userService.findByEmail(email);
        return ResponseEntity.ok(mapper.toResponse(user));
    }

    @DeleteMapping("/users")
    public ResponseEntity<Void> deleteAllUsers() {
        userService.deleteAllUsers();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUserById(@PathVariable Long id,  @AuthenticationPrincipal JwtUserPrincipal admin) {
        userService.deleteUser(admin.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{id}/update-password")
    public ResponseEntity<Void> updatePasswordById(
            @PathVariable Long id,
            @Valid @RequestBody AdminPasswordUpdateRequest request,
            @AuthenticationPrincipal JwtUserPrincipal admin) {

        userService.updatePasswordById(admin.userId(), id, request.newPassword());
        return ResponseEntity.noContent().build();
    }


    @PatchMapping("/users/{id}/update-email")
    public ResponseEntity<Void> updateEmailById(
            @PathVariable Long id,
            @Valid @RequestBody AdminEmailUpdateRequest request,
            @AuthenticationPrincipal JwtUserPrincipal admin) {

        userService.updateEmailById(admin.userId(), id, request.email());
        return ResponseEntity.noContent().build();
    }
}
