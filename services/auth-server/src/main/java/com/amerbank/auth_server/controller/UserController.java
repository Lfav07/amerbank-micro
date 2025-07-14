package com.amerbank.auth_server.controller;

import com.amerbank.auth_server.dto.*;
import com.amerbank.auth_server.model.User;
import com.amerbank.auth_server.security.JwtService;
import com.amerbank.auth_server.service.UserService;
import com.amerbank.common_dto.AuthenticationResponse;
import com.amerbank.common_dto.UserLoginRequest;
import com.amerbank.common_dto.UserRegisterRequest;
import com.amerbank.common_dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private  final UserDetailsService userDetailsService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody UserRegisterRequest request) {
        User user = userService.registerUser(request);

        UserResponse response = new UserResponse(user.getId(), user.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginRequest request) {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());

            String token = jwtService.generateToken(userDetails);

            return ResponseEntity.ok(new AuthenticationResponse(token));

        }

    @PatchMapping("/update-email")
    public ResponseEntity<?> updateEmail(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody EmailUpdateRequest request) {

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userDetails.getUsername(), request.password())
            );

            User user = userService.findByEmail(userDetails.getUsername());
            userService.updateEmail(user.getId(), request.newEmail());

            return ResponseEntity.ok("Email updated successfully");

        }


    @PatchMapping("/update-password")
    public ResponseEntity<?> updatePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody PasswordUpdateRequest request) {

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userDetails.getUsername(), request.oldPassword())
            );

            User user = userService.findByEmail(userDetails.getUsername());
            userService.updatePassword(user.getId(), request.newPassword());

            return ResponseEntity.noContent().build();

    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody PasswordRequest request) {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userDetails.getUsername(), request.password())
            );

            User user = userService.findByEmail(userDetails.getUsername());
            userService.deleteUser(user.getId());

            return  ResponseEntity.ok("User deleted successfully");

        }




    @DeleteMapping("/manage/delete/{id}")
    public  ResponseEntity<?> deleteUserById(@PathVariable Long id) {
        userService.deleteUser(id);
        return  ResponseEntity.ok("User deleted successfully");
    }

    @PatchMapping("/manage/update-password/{id}")
    public ResponseEntity<?> updatePasswordById(Long id,
            @RequestBody String password) {



        User user = userService.findById(id);
        userService.updatePassword(user.getId(), password);

        return ResponseEntity.noContent().build();

    }



    @GetMapping("/manage/by-id/{id}")
    public  ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        User user = userService.findById(id);
        UserResponse response = new UserResponse(user.getId(), user.getEmail());
        return  ResponseEntity.ok(response);
    }

    @GetMapping("/manage/by-email/{email}")
    public  ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
     User user  = userService.findByEmail(email);
        UserResponse response = new UserResponse(user.getId(), user.getEmail());
     return  ResponseEntity.ok(response);
    }


}
