package com.amerbank.auth_server.controller;

import com.amerbank.auth_server.dto.*;
import com.amerbank.auth_server.model.User;
import com.amerbank.auth_server.security.JwtService;
import com.amerbank.auth_server.service.CustomerServiceClient;
import com.amerbank.auth_server.service.UserService;
import com.amerbank.common_dto.AuthenticationResponse;
import com.amerbank.common_dto.UserLoginRequest;
import com.amerbank.common_dto.UserRegisterRequest;
import com.amerbank.common_dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;



    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody UserRegisterRequest request) {
        userService.registerUser(request);
        Map<String, String> response = Map.of("message", "User successfully registered");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody UserLoginRequest request) {
        AuthenticationResponse response = userService.login(request);
        return ResponseEntity.ok(Map.of("token", response.token()));
    }

    @PatchMapping("/update-email")
    public ResponseEntity<Map<String, String>> updateEmail(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody EmailUpdateRequest request) {


            User user = userService.findByEmail(userDetails.getUsername());
            userService.updateEmail(user.getId(), request.newEmail());

        Map<String, String> response = Map.of("message", "Email successfully updated");

            return ResponseEntity.status(HttpStatus.OK).body(response);

        }


    @PatchMapping("/update-password")
    public  ResponseEntity<Map<String, String>> updatePassword(
            @AuthenticationPrincipal UserDetails userDetails,
           @Valid @RequestBody PasswordUpdateRequest request) {

        try {
            userService.updatePassword(userDetails.getUsername(), request);
            return ResponseEntity.ok(Map.of("message", "Password successfully updated"));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Old password is incorrect"));
        }
    }

    @GetMapping("/all/get")
    public ResponseEntity<List<User>> getAllUsers() {
         List<User> users = userService.getAllUsers();
        return  ResponseEntity.ok(users);

    }

    @DeleteMapping("/all/delete")
    public ResponseEntity<Map<String, String>> deleteAllUsers() {
        userService.deleteAllUsers();
        return  ResponseEntity.ok(Map.of("message", "Users successfully deleted"));

    }




    @DeleteMapping("/manage/delete/{id}")
    public  ResponseEntity<Map<String, String>> deleteUserById(@PathVariable Long id) {

        userService.deleteUser(id);
        return  ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    @PatchMapping("/manage/update-password/{id}")
    public ResponseEntity<?> updatePasswordById(
            @PathVariable Long id,
            @RequestBody @Valid AdminPasswordUpdateRequest request) {

        userService.updatePasswordById(id, request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/manage/update-email/{id}")
    public ResponseEntity<?> updateEmailById(
            @PathVariable Long id,
            @RequestBody @Valid AdminEmailUpdateRequest request) {

        userService.updateEmailById(id, request.email());
        return ResponseEntity.noContent().build();
    }




    @GetMapping("/manage/by-id/{id}")
    public  ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        User user = userService.findById(id);
        UserResponse response = new UserResponse(user.getId(), user.getEmail());
        return  ResponseEntity.ok(response);
    }

    @GetMapping("/manage/me")
    public ResponseEntity<UserResponse> getMyUserInfo(Authentication authentication) {
        String email = authentication.getName();
        User user = userService.findByEmail(email);
        UserResponse resp = new UserResponse(user.getId(), user.getEmail());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/manage/by-email/{email}")
    public  ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
     User user  = userService.findByEmail(email);
        UserResponse response = new UserResponse(user.getId(), user.getEmail());
     return  ResponseEntity.ok(response);
    }


}
