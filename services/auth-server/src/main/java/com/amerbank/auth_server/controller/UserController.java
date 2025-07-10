package com.amerbank.auth_server.controller;

import com.amerbank.auth_server.UserNotFoundException;
import com.amerbank.auth_server.dto.*;
import com.amerbank.auth_server.model.User;
import com.amerbank.auth_server.security.JwtService;
import com.amerbank.auth_server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRegisterRequest request) {
        User user = userService.registerUser(request);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            User user = userService.findByEmail(request.email());
            String token = jwtService.generateToken(user);
            return ResponseEntity.ok(new AuthenticationResponse(token));

        } catch (AuthenticationException ex) {
            return ResponseEntity.status(401).body("Unauthorized Access");
        }
    }

    @PatchMapping("/update-email")
    public ResponseEntity<?> updateEmail(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody EmailUpdateRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userDetails.getUsername(), request.password())
            );

            User user = userService.findByEmail(userDetails.getUsername());
            userService.updateEmail(user.getId(), request.newEmail());

            return ResponseEntity.noContent().build();

        } catch (AuthenticationException ex) {
            return ResponseEntity.status(401).body("Unauthorized Access");
        }
    }

    @PatchMapping("/update-password")
    public ResponseEntity<?> updatePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody PasswordUpdateRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userDetails.getUsername(), request.oldPassword())
            );

            User user = userService.findByEmail(userDetails.getUsername());
            userService.updatePassword(user.getId(), request.newPassword());

            return ResponseEntity.noContent().build();

        } catch (AuthenticationException ex) {
            return ResponseEntity.status(401).body("Unauthorized Access");
        }
    }

    @PostMapping("/delete")
    public ResponseEntity<?> deleteUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody PasswordRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userDetails.getUsername(), request.password())
            );

            User user = userService.findByEmail(userDetails.getUsername());
            userService.deleteUser(user.getId());

            return ResponseEntity.noContent().build();

        } catch (AuthenticationException ex) {
            return ResponseEntity.status(401).body("Unauthorized Access");
        }
    }
}
