package com.amerbank.customer.customer.controller;

import com.amerbank.customer.customer.dto.CustomerRegistrationRequest;
import com.amerbank.customer.customer.dto.CustomerRegistrationResponse;
import com.amerbank.customer.customer.dto.ErrorResponse;
import com.amerbank.customer.customer.dto.ValidationErrorResponse;
import com.amerbank.customer.customer.service.CustomerService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/customer/internal")
@Tag(
        name = "Internal - Customers",
        description = "Internal endpoints for service-to-service communication. Requires service JWT token."
)
public class CustomerInternalController {
    private final CustomerService service;


    @Operation(
            summary = "Register customer (internal)",
            description = """
                    Creates a new customer profile linked to an existing user.
                    
                    **Authorization:** Requires valid service JWT token (service-to-service).
                    
                    **Business Rules:**
                    - Each user can have at most one customer profile (one-to-one relationship)
                    - Customer is linked to user via `userId`
                    - Profile is created automatically during user registration flow
                    - KYC is set to verified by default for internal registrations
                    
                    **Request Body Fields:**
                    - `userId`: Associated user ID from auth service (required)
                    - `firstName`: Customer first name (required)
                    - `lastName`: Customer last name (required)
                    - `dateOfBirth`: Customer date of birth (required)
                    """,
            security = @SecurityRequirement(name = "Bearer JWT")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Customer successfully registered",
                    content = @Content(
                            schema = @Schema(implementation = CustomerRegistrationResponse.class),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "customerId": 1
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request body or validation failed",
                    content = @Content(
                            schema = @Schema(implementation = ValidationErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Validation Error",
                                    value = """
                                            {
                                              "timestamp": "2024-01-15T10:30:00",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "Validation failed",
                                              "errors": {
                                                "firstName": "First name is required",
                                                "dateOfBirth": "Date of birth is required"
                                              },
                                              "path": "/customer/internal/register",
                                              "traceId": "abc123"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid or missing service JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Customer already exists for the given user ID",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Conflict Error",
                                    value = """
                                            {
                                              "timestamp": "2024-01-15T10:30:00",
                                              "status": 409,
                                              "error": "Conflict",
                                              "message": "Customer already exists",
                                              "path": "/customer/internal/register",
                                              "traceId": "abc123"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/register")
    public ResponseEntity<CustomerRegistrationResponse> registerCustomer(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Customer registration request containing user linkage and profile details",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CustomerRegistrationRequest.class),
                            examples = @ExampleObject(
                                    name = "Request Body",
                                    value = """
                                            {
                                              "userId": 100,
                                              "firstName": "John",
                                              "lastName": "Doe",
                                              "dateOfBirth": "1990-01-15"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody @Valid CustomerRegistrationRequest request) {
        CustomerRegistrationResponse response = service.registerCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Get customer ID by user ID (internal)",
            description = """
                    Retrieves the customer ID associated with a user ID.
                    
                    **Authorization:** Requires valid service JWT token (service-to-service).
                    
                    **Path Parameters:**
                    - `userId`: The user ID to look up
                    
                    **Business Rules:**
                    - Returns the customer ID linked to the given user
                    - Used by other services to resolve user-customer relationship
                    """,
            security = @SecurityRequirement(name = "Bearer JWT")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Customer ID found",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = "1"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid or missing service JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Customer not found for the given user ID",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<Long> getCustomerIdByUserId(@PathVariable Long userId) {
        Long id = service.getCustomerIdByUserId(userId);
        return ResponseEntity.ok(id);
    }
}
