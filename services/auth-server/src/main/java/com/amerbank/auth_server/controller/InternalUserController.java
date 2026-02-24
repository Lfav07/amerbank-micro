package com.amerbank.auth_server.controller;

import com.amerbank.auth_server.dto.ErrorResponse;
import com.amerbank.auth_server.dto.UserRegisterRequest;
import com.amerbank.auth_server.dto.UserResponse;
import com.amerbank.auth_server.dto.ValidationErrorResponse;
import com.amerbank.auth_server.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/internal")
@Tag(name = "Auth", description = "Authentication and authorization endpoints")
public class InternalUserController {

    private final UserService userService;

    @Operation(
            summary = "Get user by email (internal)",
            description = """
                    Retrieves a user by their email address for service-to-service communication.
                    
                    This endpoint is intended for internal microservices use only.
                    It requires a valid service JWT token from an authenticated service.
                    
                    **Authentication:** Required (service-to-service)
                    **Authorization:** Requires valid service JWT token
                    
                    **Use case:** Other microservices querying user information by email.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class),
                            examples = @ExampleObject(
                                    name = "User Found",
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
                    description = "Bad Request - Invalid email format",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ValidationErrorResponse.class)
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
    public ResponseEntity<UserResponse> InternalGetUserByEmail(
            @RequestParam @Email String email) {
        return ResponseEntity.ok(userService.findByEmailMapped(email));
    }
}
