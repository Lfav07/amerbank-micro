package com.amerbank.auth_server.controller;

import com.amerbank.auth_server.dto.*;
import com.amerbank.auth_server.security.JwtUserPrincipal;
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
import jakarta.validation.constraints.Email;
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
@Tag(name = "Auth", description = "Authentication and authorization endpoints")
public class AdminController {

    private final UserService userService;

    private static final String JWT_SCHEME = "Bearer JWT";

    private Map<String, String> message(String msg) {
        return Map.of("message", msg);
    }

    @Operation(
            summary = "Register admin user",
            description = """
                    Creates a new administrator account with ROLE_ADMIN privileges.
                    
                    This endpoint is enabled for demonstration and testing purposes.
                    In production, admin registration should be restricted.
                    
                    **Authentication:** Not required (public endpoint)
                    
                    **Use case:** Creating administrative accounts for system management.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Admin successfully registered",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class),
                            examples = @ExampleObject(
                                    name = "Admin Registration Success",
                                    value = """
                                            {
                                              "id": 1,
                                              "customerId": null,
                                              "email": "admin@amerbank.com"
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
                            schema = @Schema(implementation = ValidationErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict - Email already exists",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerAdmin(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Admin registration credentials",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AdminRegisterRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Admin Registration Request",
                                            value = """
                                                    {
                                                      "email": "admin@amerbank.com",
                                                      "password": "admin123"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
            @Valid @RequestBody AdminRegisterRequest request) {
        UserResponse response = userService.registerAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Admin login",
            description = """
                    Authenticates an administrator with email and password credentials.
                    
                    Returns a JWT token with admin privileges for accessing administrative endpoints.
                    
                    **Authentication:** Not required (public endpoint)
                    
                    **Use case:** Admin users logging in to access management features.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful - JWT token returned",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthenticationResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid email or password",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> loginAdmin(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Admin login credentials",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserLoginRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Admin Login Request",
                                            value = """
                                                    {
                                                      "email": "admin@amerbank.com",
                                                      "password": "admin123"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
            @Valid @RequestBody UserLoginRequest request) {
        AuthenticationResponse response = userService.loginAdmin(request);
        return ResponseEntity.ok(Map.of("token", response.token()));
    }

    @Operation(
            summary = "Get all users",
            description = """
                    Retrieves all registered users in the system.
                    
                    **Authentication:** Required
                    **Authorization:** Requires ADMIN role
                    
                    **Use case:** Administrative overview of all registered users.
                    
                    **Authorization header:** `Authorization: Bearer {token}`
                    """,
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Users retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Requires ADMIN role",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Operation(
            summary = "Get user by ID",
            description = """
                    Retrieves a user by their unique identifier.
                    
                    **Authentication:** Required
                    **Authorization:** Requires ADMIN role
                    
                    **Use case:** Looking up a specific user by their system ID.
                    
                    **Authorization header:** `Authorization: Bearer {token}`
                    """,
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Requires ADMIN role",
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
    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findByIdMapped(id));
    }

    @Operation(
            summary = "Get user by email",
            description = """
                    Retrieves a user by their email address.
                    
                    **Authentication:** Required
                    **Authorization:** Requires ADMIN role
                    
                    **Use case:** Looking up a user by their email address.
                    
                    **Authorization header:** `Authorization: Bearer {token}`
                    """,
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class)
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
                    responseCode = "403",
                    description = "Forbidden - Requires ADMIN role",
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
    @GetMapping("/users/by-email")
    public ResponseEntity<UserResponse> getUserByEmail(
            @RequestParam @Email String email) {
        return ResponseEntity.ok(userService.findByEmailMapped(email));
    }

    @Operation(
            summary = "Delete all users",
            description = """
                    **DEPRECATED: This endpoint is for demo purposes only.**
                    
                    Deletes ALL users from the database. This is a destructive operation
                    that should never be used in production environments.
                    
                    **Authentication:** Required
                    **Authorization:** Requires ADMIN role
                    
                    **Authorization header:** `Authorization: Bearer {token}`
                    """,
            deprecated = true,
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "All users deleted",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "All Users Deleted",
                                    value = """
                                            {
                                              "message": "All users successfully deleted."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Requires ADMIN role",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @DeleteMapping("/users")
    public ResponseEntity<Map<String, String>> deleteAllUsers() {
        userService.deleteAllUsers();
        return ResponseEntity.ok(message("All users successfully deleted."));
    }

    @Operation(
            summary = "Delete user by ID",
            description = """
                    Permanently deletes a user by their unique identifier.
                    
                    **Authentication:** Required
                    **Authorization:** Requires ADMIN role
                    
                    **Use case:** Removing a user account from the system.
                    
                    **Authorization header:** `Authorization: Bearer {token}`
                    """,
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User deleted successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "User Deleted",
                                    value = """
                                            {
                                              "message": "Successfully deleted user 123"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Requires ADMIN role",
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
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> deleteUserById(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtUserPrincipal admin) {
        userService.deleteUser(admin.userId(), id);
        return ResponseEntity.ok(message("Successfully deleted user " + id));
    }

    @Operation(
            summary = "Update user password (admin)",
            description = """
                    Updates a user's password by their ID without requiring the current password.
                    
                    **Authentication:** Required
                    **Authorization:** Requires ADMIN role
                    
                    **Use case:** Administrative password reset for users.
                    
                    **Authorization header:** `Authorization: Bearer {token}`
                    """,
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Password updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Password Updated",
                                    value = """
                                            {
                                              "message": "Password successfully updated for user 123"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Invalid request",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ValidationErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Requires ADMIN role",
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
    @PatchMapping("/users/{id}/password")
    public ResponseEntity<Map<String, String>> updateUserPassword(
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New password for the user",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AdminPasswordUpdateRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Admin Password Update Request",
                                            value = """
                                                    {
                                                      "newPassword": "newpassword456"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
            @Valid @RequestBody AdminPasswordUpdateRequest request,
            @AuthenticationPrincipal JwtUserPrincipal admin) {
        userService.updatePasswordById(admin.userId(), id, request.newPassword());
        return ResponseEntity.ok(message("Password successfully updated for user " + id));
    }

    @Operation(
            summary = "Update user email (admin)",
            description = """
                    Updates a user's email address by their ID.
                    
                    **Authentication:** Required
                    **Authorization:** Requires ADMIN role
                    
                    **Use case:** Administrative email update for users.
                    
                    **Authorization header:** `Authorization: Bearer {token}`
                    """,
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Email updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Email Updated",
                                    value = """
                                            {
                                              "message": "Email successfully updated for user 123"
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
                    responseCode = "403",
                    description = "Forbidden - Requires ADMIN role",
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
    @PatchMapping("/users/{id}/email")
    public ResponseEntity<Map<String, String>> updateUserEmail(
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New email for the user",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AdminEmailUpdateRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Admin Email Update Request",
                                            value = """
                                                    {
                                                      "newEmail": "newemail@example.com"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
            @Valid @RequestBody AdminEmailUpdateRequest request,
            @AuthenticationPrincipal JwtUserPrincipal admin) {
        userService.updateEmailById(admin.userId(), id, request.newEmail());
        return ResponseEntity.ok(message("Email successfully updated for user " + id));
    }
}
