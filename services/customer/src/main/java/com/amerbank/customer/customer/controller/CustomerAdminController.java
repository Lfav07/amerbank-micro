package com.amerbank.customer.customer.controller;

import com.amerbank.customer.customer.dto.CustomerResponse;
import com.amerbank.customer.customer.dto.CustomerUpdateRequest;
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
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/customer/admin")
@Tag(name = "Admin - Customers", description = "Administrative endpoints for customer management. Requires ADMIN role.")
public class CustomerAdminController {
    private final CustomerService service;

    private Map<String, String> message(String msg) {
        return Map.of("message", msg);
    }


    @Operation(
            summary = "Get all customers",
            description = """
                    Retrieves all registered customer profiles.
                    
                    **Authorization:** Requires ADMIN role.
                    
                    **Business Rules:**
                    - Returns all customers in the system
                    - Each customer is linked to a user via `userId`
                    - Response includes KYC verification status
                    """,
            security = @SecurityRequirement(name = "Bearer JWT")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Customers retrieved successfully",
                    content = @Content(
                            schema = @Schema(implementation = CustomerResponse.class),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            [
                                              {
                                                "id": 1,
                                                "userId": 100,
                                                "firstName": "John",
                                                "lastName": "Doe",
                                                "dateOfBirth": "1990-01-15",
                                                "kycVerified": true,
                                                "createdAt": "2024-01-15T10:30:00"
                                              }
                                            ]
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/customers")
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        List<CustomerResponse> responseList = service.findAllCustomers();
        return ResponseEntity.ok(responseList);
    }

    @Operation(
            summary = "Get customer by ID",
            description = """
                    Retrieves a customer profile by their unique identifier.
                    
                    **Authorization:** Requires ADMIN role.
                    
                    **Path Parameters:**
                    - `id`: Customer unique identifier
                    
                    **Response Fields:**
                    - `id`: Customer unique ID
                    - `userId`: Associated user ID from auth service
                    - `firstName`, `lastName`: Customer name
                    - `dateOfBirth`: Date of birth
                    - `kycVerified`: KYC verification status
                    - `createdAt`: Profile creation timestamp
                    """,
            security = @SecurityRequirement(name = "Bearer JWT")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Customer found",
                    content = @Content(
                            schema = @Schema(implementation = CustomerResponse.class),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "id": 1,
                                              "userId": 100,
                                              "firstName": "John",
                                              "lastName": "Doe",
                                              "dateOfBirth": "1990-01-15",
                                              "kycVerified": true,
                                              "createdAt": "2024-01-15T10:30:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Customer not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/customers/{id}")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long id) {
        CustomerResponse response = service.getCustomerInfo(id);
        return ResponseEntity.ok(response);
    }


    @Operation(
            summary = "Get customer by email",
            description = """
                    Retrieves a customer profile by their email address.
                    
                    **Authorization:** Requires ADMIN role.
                    
                    **Query Parameters:**
                    - `email`: Customer's registered email address
                    
                    **Business Rules:**
                    - Looks up user in auth service by email
                    - Returns customer linked to that user
                    """,
            security = @SecurityRequirement(name = "Bearer JWT")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Customer found",
                    content = @Content(
                            schema = @Schema(implementation = CustomerResponse.class),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "id": 1,
                                              "userId": 100,
                                              "firstName": "John",
                                              "lastName": "Doe",
                                              "dateOfBirth": "1990-01-15",
                                              "kycVerified": true,
                                              "createdAt": "2024-01-15T10:30:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid email format",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Customer not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/customers/by-email")
    public ResponseEntity<CustomerResponse> getCustomerByEmail(@RequestParam @Email String email) {
        CustomerResponse resp = service.getCustomerInfoByEmail(email);
        return ResponseEntity.ok(resp);
    }


    @Operation(
            summary = "Update customer",
            description = """
                    Updates customer profile information.
                    
                    **Authorization:** Requires ADMIN role.
                    
                    **Path Parameters:**
                    - `id`: Customer unique identifier
                    
                    **Request Body:**
                    - `firstName`: Customer's first name (required)
                    - `lastName`: Customer's last name (required)
                    
                    **Business Rules:**
                    - Only modifies name fields
                    - Does not change KYC status or user association
                    """,
            security = @SecurityRequirement(name = "Bearer JWT")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Customer updated successfully",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "message": "Successfully updated customer's data for customer 1"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request body - validation failed",
                    content = @Content(
                            schema = @Schema(implementation = ValidationErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Validation Error",
                                            value = """
                                                    {
                                                      "timestamp": "2024-01-15T10:30:00",
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "Validation failed",
                                                      "errors": {
                                                        "firstName": "First name is required"
                                                      },
                                                      "path": "/customer/admin/customers/1",
                                                      "traceId": "abc123"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Customer not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PutMapping("/customers/{id}")
    public ResponseEntity<Map<String, String>> updateCustomer(
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Customer update request containing first and last name",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CustomerUpdateRequest.class),
                            examples = @ExampleObject(
                                    name = "Request Body",
                                    value = """
                                            {
                                              "firstName": "John",
                                              "lastName": "Smith"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody @Valid CustomerUpdateRequest request) {
        service.editCustomerInfo(id, request);
        return ResponseEntity.ok(message("Successfully updated customer's data for customer " + id));
    }


    @Operation(
            summary = "Verify customer KYC",
            description = """
                    Marks customer's KYC (Know Your Customer) verification status as verified.
                    
                    **Authorization:** Requires ADMIN role.
                    
                    **Path Parameters:**
                    - `id`: Customer unique identifier
                    
                    **Business Rules:**
                    - Sets KYC status to verified
                    - Used for manual verification processes
                    """,
            security = @SecurityRequirement(name = "Bearer JWT")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "KYC verified successfully",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "message": "KYC verified successfully"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Customer not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PatchMapping("/customers/{id}/kyc/verify")
    public ResponseEntity<Map<String, String>> verifyKyc(@PathVariable Long id) {
        service.verifyKyc(id);
        return ResponseEntity.ok(message("KYC verified successfully"));
    }

    @Operation(
            summary = "Revoke customer KYC",
            description = """
                    Revokes customer's KYC verification status.
                    
                    **Authorization:** Requires ADMIN role.
                    
                    **Path Parameters:**
                    - `id`: Customer unique identifier
                    
                    **Business Rules:**
                    - Sets KYC status to not verified
                    - Used when verification needs to be reconsidered
                    """,
            security = @SecurityRequirement(name = "Bearer JWT")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "KYC revoked successfully",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "message": "KYC unverified successfully"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Customer not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PatchMapping("/customers/{id}/kyc/revoke")
    public ResponseEntity<Map<String, String>> unVerifyKyc(@PathVariable Long id) {
        service.revokeKyc(id);
        return ResponseEntity.ok(message("KYC unverified successfully"));
    }


    @Operation(
            summary = "Delete customer",
            description = """
                    Permanently deletes a customer profile by their ID.
                    
                    **Authorization:** Requires ADMIN role.
                    
                    **Path Parameters:**
                    - `id`: Customer unique identifier
                    
                    **Business Rules:**
                    - Permanently removes customer record
                    - Does not delete the associated user in auth service
                    """,
            security = @SecurityRequirement(name = "Bearer JWT")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Customer deleted successfully",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "message": "Successfully deleted customer 1"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Customer not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/customers/{id}")
    public ResponseEntity<Map<String, String>> deleteCustomer(@PathVariable Long id) {
        service.deleteCustomerById(id);
        return ResponseEntity.ok(message("Successfully deleted customer " + id));
    }
}
