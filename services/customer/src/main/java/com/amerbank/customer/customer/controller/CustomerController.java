package com.amerbank.customer.customer.controller;

import com.amerbank.customer.customer.dto.CustomerInfo;
import com.amerbank.customer.customer.dto.ErrorResponse;
import com.amerbank.customer.customer.security.JwtUserPrincipal;
import com.amerbank.customer.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/customer")
@Tag(name = "Customers", description = "Customer profile management")
public class CustomerController {

    private final CustomerService service;


    @Operation(
            summary = "Get current customer profile",
            description = """
                    Retrieves the profile information of the authenticated customer.
                    
                    **Business Rules:**
                    - Each authenticated user can have at most one customer profile
                    - The customer profile is linked to the user via the `userId` field
                    - Only the owner of the profile can access their own data
                    - Profile is created during user registration through internal service call
                    """,
            security = @SecurityRequirement(name = "Bearer JWT")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Customer profile retrieved successfully",
                    content = @Content(
                            schema = @Schema(implementation = CustomerInfo.class),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "id": 1,
                                              "firstName": "John",
                                              "lastName": "Doe",
                                              "dateOfBirth": "1990-01-15"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Customer not found - no profile exists for the authenticated user",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/me")
    public ResponseEntity<CustomerInfo> getMyProfile(@AuthenticationPrincipal JwtUserPrincipal principal) {
        CustomerInfo resp = service.getMyCustomerInfo(principal.customerId());
        return ResponseEntity.ok(resp);
    }
}
