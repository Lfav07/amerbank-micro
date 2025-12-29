package com.amerbank.auth_server.controller;

import com.amerbank.auth_server.dto.AdminEmailUpdateRequest;
import com.amerbank.auth_server.dto.AdminPasswordUpdateRequest;
import com.amerbank.auth_server.security.JwtUserPrincipal;
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

    // -------------------- Helper --------------------
    private Map<String, String> message(String msg) {
        return Map.of("message", msg);
    }

    // -------------------- Admin Authentication --------------------

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registerAdmin(
            @Valid @RequestBody UserRegisterRequest request) {

        userService.registerAdmin(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(message("Admin successfully registered."));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> loginAdmin(
            @Valid @RequestBody UserLoginRequest request) {

        AuthenticationResponse response = userService.loginAdmin(request);

        return ResponseEntity.ok(
                Map.of("token", response.token())
        );
    }

// -------------------- User Management --------------------

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findByIdMapped(id));
    }

    @GetMapping("/users/by-email")
    public ResponseEntity<UserResponse> getUserByEmail(
            @RequestParam String email) {

        return ResponseEntity.ok(userService.findByEmailMapped(email));
    }

// -------------------- Demo Only --------------------

    @DeleteMapping("/users")
    public ResponseEntity<Map<String, String>> deleteAllUsers() {

        userService.deleteAllUsers();

        return ResponseEntity.ok(
                message("All users successfully deleted.")
        );
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> deleteUserById(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtUserPrincipal admin) {

        userService.deleteUser(admin.userId(), id);

        return ResponseEntity.ok(
                message("Successfully deleted user " + id)
        );
    }

// -------------------- Updates --------------------

    @PatchMapping("/users/{id}/password")
    public ResponseEntity<Map<String, String>> updateUserPassword(
            @PathVariable Long id,
            @Valid @RequestBody AdminPasswordUpdateRequest request,
            @AuthenticationPrincipal JwtUserPrincipal admin) {


        userService.updatePasswordById(admin.userId(), id, request.newPassword());

        return ResponseEntity.ok(
                message("Password successfully updated for user " + id)
        );
    }

    @PatchMapping("/users/{id}/email")
    public ResponseEntity<Void> updateUserEmail(
            @PathVariable Long id,
            @Valid @RequestBody AdminEmailUpdateRequest request,
            @AuthenticationPrincipal JwtUserPrincipal admin) {


        userService.updateEmailById(admin.userId(), id, request.email());

        return ResponseEntity.noContent().build();
    }
}
