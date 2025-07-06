package com.amerbank.auth_server.controller;

import com.amerbank.auth_server.UserNotFoundException;
import com.amerbank.auth_server.dto.UserRegisterRequest;
import com.amerbank.auth_server.dto.UserLoginRequest;
import com.amerbank.auth_server.dto.AuthenticationResponse;
import com.amerbank.auth_server.model.User;
import com.amerbank.auth_server.security.JwtService;
import com.amerbank.auth_server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
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
        // Register user in DB
        User user = userService.registerUser(request);

        // Generate JWT for new user
        String token = jwtService.generateToken(user);

        return ResponseEntity.ok(new AuthenticationResponse(token));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginRequest request) {
        try {

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );


            User user = userService.findByEmail(request.email());

            // Generate JWT token
            String token = jwtService.generateToken(user);

            return ResponseEntity.ok(new AuthenticationResponse(token));

        } catch (AuthenticationException ex) {
            return ResponseEntity.status(401).body("Invalid email or password");
        } catch (UserNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
