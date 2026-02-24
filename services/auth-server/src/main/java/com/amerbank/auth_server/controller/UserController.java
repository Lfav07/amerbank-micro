package com.amerbank.auth_server.controller;

import com.amerbank.auth_server.dto.*;
import com.amerbank.auth_server.security.JwtUserPrincipal;
import com.amerbank.auth_server.service.UserMapper;
import com.amerbank.auth_server.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication and authorization endpoints")
public class UserController {

    private final UserService userService;
    private final UserMapper mapper;

    private static final String JWT_SCHEME = "Bearer JWT";

    private Map<String, String> message(String msg) {
        return Map.of("message", msg);
    }

    @Operation(
            summary = "User login",
            description = """
                    Authenticates a user with email and password credentials.
                    
                    Use this endpoint to obtain a JWT token for accessing protected resources.
                    The returned token must be included in the Authorization header for subsequent API calls.
                    
                    **Authentication:** Not required (public endpoint)
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful - JWT token returned",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthenticationResponse.class),
                            examples = @ExampleObject(
                                    name = "Login Success",
                                    value = """
                                            {
                                              "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwiY3VzdG9tZXJJZCI6MTAwLCJyb2xlcyI6WyJST0xFX1VTRVIiXSwiaWF0IjoxNzA0MDY3MjAwLCJleHAiOjE3MDQxNTM2MDB9.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid email or password",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Invalid Credentials",
                                    value = """
                                            {
                                              "timestamp": "2026-02-23T10:30:00",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Incorrect username or password",
                                              "path": "/auth/login",
                                              "traceId": "abc123"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User login credentials",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserLoginRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Login Request",
                                            value = """
                                                    {
                                                      "email": "john.doe@example.com",
                                                      "password": "password123"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
            @Valid @RequestBody UserLoginRequest request) {
        AuthenticationResponse response = userService.login(request);
        return ResponseEntity.ok(Map.of("token", response.token()));
    }

    @Operation(
            summary = "Register new user",
            description = """
                    Creates a new user account with ROLE_USER privileges.
                    
                    Upon successful registration, a corresponding customer profile is automatically
                    created in the customer-service. This links the authentication account
                    with the customer's profile data.
                    
                    **Authentication:** Not required (public endpoint)
                    
                    **Use case:** Use this endpoint for new user onboarding.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "User successfully registered",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class),
                            examples = @ExampleObject(
                                    name = "Registration Success",
                                    value = """
                                            {
                                              "id": 1,
                                              "customerId": 100,
                                              "email": "john.doe@example.com"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Validation failed",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ValidationErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Validation Error",
                                    value = """
                                            {
                                              "timestamp": "2026-02-23T10:30:00",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "Validation failed",
                                              "errors": {
                                                "email": "must be a well-formed email address",
                                                "password": "Password too short",
                                                "firstName": "First name is required"
                                              },
                                              "path": "/auth/register",
                                              "traceId": "abc123"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict - Email already exists",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Email Conflict",
                                    value = """
                                            {
                                              "timestamp": "2026-02-23T10:30:00",
                                              "status": 409,
                                              "error": "Conflict",
                                              "message": "Email already taken",
                                              "path": "/auth/register",
                                              "traceId": "abc123"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User registration details. Password must be at least 4 characters.",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserRegisterRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Registration Request",
                                            value = """
                                                    {
                                                      "email": "john.doe@example.com",
                                                      "password": "password123",
                                                      "firstName": "John",
                                                      "lastName": "Doe",
                                                      "dateOfBirth": "1990-01-15"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
            @Valid @RequestBody UserRegisterRequest request) {
        UserResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Update user email",
            description = """
                    Updates the authenticated user's email address.
                    
                    This endpoint allows users to change their own email. The new email must be unique.
                    
                    **Authentication:** Required
                    **Authorization:** Must be the authenticated user
                    
                    **Use case:** When a user wants to change their email address.
                    
                    **Authorization header:** `Authorization: Bearer {token}`
                    """,
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Email successfully updated",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Email Updated",
                                    value = """
                                            {
                                              "message": "Email successfully updated"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Invalid email format",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ValidationErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict - Email already taken",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PatchMapping("/me/email")
    public ResponseEntity<Map<String, String>> updateEmail(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New email address",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = EmailUpdateRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Email Update Request",
                                            value = """
                                                    {
                                                      "newEmail": "newemail@example.com"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
            @Valid @RequestBody EmailUpdateRequest request) {
        userService.updateEmail(principal.userId(), request.newEmail());
        return ResponseEntity.ok(message("Email successfully updated"));
    }

    @Operation(
            summary = "Update user password",
            description = """
                    Updates the authenticated user's password.
                    
                    Requires the current password for verification before setting a new password.
                    
                    **Authentication:** Required
                    **Authorization:** Must be the authenticated user
                    
                    **Use case:** When a user wants to change their password for security reasons.
                    
                    **Authorization header:** `Authorization: Bearer {token}`
                    """,
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Password successfully updated",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Password Updated",
                                    value = """
                                            {
                                              "message": "Password successfully updated"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Invalid request or incorrect current password",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ValidationErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PatchMapping("/me/password")
    public ResponseEntity<Map<String, String>> updatePassword(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Current and new password",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PasswordUpdateRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Password Update Request",
                                            value = """
                                                    {
                                                      "currentPassword": "oldpassword123",
                                                      "newPassword": "newpassword456"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
            @Valid @RequestBody PasswordUpdateRequest request) {
        userService.updatePassword(principal.userId(), request);
        return ResponseEntity.ok(message("Password successfully updated"));
    }

    @Operation(
            summary = "Get current user info",
            description = """
                    Retrieves the profile information of the currently authenticated user.
                    
                    Returns the user's ID, customer ID, and email address.
                    
                    **Authentication:** Required
                    **Authorization:** Must be the authenticated user
                    
                    **Use case:** To fetch the authenticated user's own profile data.
                    
                    **Authorization header:** `Authorization: Bearer {token}`
                    """,
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User profile retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class),
                            examples = @ExampleObject(
                                    name = "User Profile",
                                    value = """
                                            {
                                              "id": 1,
                                              "customerId": 100,
                                              "email": "john.doe@example.com"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found - User not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyUserInfo(@AuthenticationPrincipal JwtUserPrincipal principal) {
        UserResponse response = userService.getOwnUserInfo(principal.userId());
        return ResponseEntity.ok(response);
    }
}
