package com.amerbank.account.controller;

import com.amerbank.account.dto.*;
import com.amerbank.account.model.AccountType;
import com.amerbank.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST controller for administrative account operations.
 * Handles account management, status updates, and administrative queries.
 * All endpoints require ADMIN role as configured in SecurityConfig.
 */
@RestController
@RequestMapping("/account/admin")
@RequiredArgsConstructor
@Validated
@Tag(name = "Admin Management", description = "Administrative endpoints for account management. Requires ADMIN role.")
public class AccountAdminController {

    private final AccountService accountService;

    private static final String JWT_SCHEME = "Bearer JWT";

    // ============================================================
    // ================= ACCOUNT RETRIEVAL ========================
    // ============================================================

    @Operation(
            summary = "Get all accounts for a customer",
            description = """
                    Retrieves all bank accounts for a specific customer by customer ID.
                    
                    **Authentication:** Required
                    **Authorization:** Requires ADMIN role
                    
                    **Use case:** When an admin needs to view all accounts belonging to a specific customer.
                    
                    **Authorization header:** `Authorization: Bearer {token}`
                    """,
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Account type updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AccountResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - validation failed or customer already has account of new type",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ValidationErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - requires ADMIN role",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found - account not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PutMapping("/{accountNumber}/type")
    public ResponseEntity<AccountResponse> updateAccountType(
            @Parameter(description = "Account number", required = true)
            @PathVariable @NotBlank String accountNumber,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New account type",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = AccountUpdateTypeRequest.class),
                            examples = @ExampleObject(value = "{\"type\":\"SAVINGS\"}")
                    )
            )
            @Valid @RequestBody AccountUpdateTypeRequest request) {

        AccountResponse updatedAccount = accountService.updateAccountType(accountNumber, request);
        return ResponseEntity.ok(updatedAccount);
    }

    @Operation(
            summary = "Update account status",
            description = """
                    Updates the status of an account (ACTIVE, SUSPENDED, CLOSED).
                    
                    **Authentication:** Required
                    **Authorization:** Requires ADMIN role
                    
                    **Use case:** When an admin needs to suspend or close an account.
                    
                    **Authorization header:** `Authorization: Bearer {token}`
                    """,
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Account status updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AccountResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - invalid status value",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ValidationErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - requires ADMIN role",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found - account not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PutMapping("/{accountNumber}/status")
    public ResponseEntity<AccountResponse> updateAccountStatus(
            @Parameter(description = "Account number", required = true)
            @PathVariable @NotBlank String accountNumber,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New account status",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = AccountUpdateStatusRequest.class),
                            examples = @ExampleObject(value = "{\"status\":\"SUSPENDED\"}")
                    )
            )
            @Valid @RequestBody AccountUpdateStatusRequest request) {

        AccountResponse updatedAccount = accountService.updateAccountStatus(accountNumber, request);
        return ResponseEntity.ok(updatedAccount);
    }

    @Operation(
            summary = "Suspend account",
            description = """
                    Suspends a specific account by setting its status to SUSPENDED.
                    
                    **Authentication:** Required
                    **Authorization:** Requires ADMIN role
                    
                    **Use case:** When an admin needs to temporarily disable an account.
                    
                    **Authorization header:** `Authorization: Bearer {token}`
                    """,
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Account suspended successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AccountResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - requires ADMIN role",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found - account not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PutMapping("/{accountNumber}/suspend")
    public ResponseEntity<AccountResponse> suspendAccount(
            @Parameter(description = "Account number", required = true)
            @PathVariable @NotBlank String accountNumber) {

        AccountResponse suspendedAccount = accountService.suspendAccount(accountNumber);
        return ResponseEntity.ok(suspendedAccount);
    }

    @Operation(
            summary = "Delete account",
            description = """
                    Permanently deletes an account by its account number.
                    
                    **Authentication:** Required
                    **Authorization:** Requires ADMIN role
                    
                    **Use case:** When an admin needs to permanently remove an account from the system.
                    
                    **Authorization header:** `Authorization: Bearer {token}`
                    """,
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Account deleted successfully"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - requires ADMIN role",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found - account not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @DeleteMapping("/{accountNumber}")
    public ResponseEntity<Void> deleteAccount(
            @Parameter(description = "Account number to delete", required = true)
            @PathVariable @NotBlank String accountNumber){

        accountService.deleteAccount(accountNumber);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // ============= ACCOUNT STATUS & OWNERSHIP CHECKS ============
    // ============================================================

    @Operation(
            summary = "Check if account is active",
            description = """
                    Verifies if an account is currently active.
                    
                    **Authentication:** Required
                    **Authorization:** Requires ADMIN role
                    
                    **Use case:** When an admin needs to verify if an account is active before processing.
                    
                    **Authorization header:** `Authorization: Bearer {token}`
                    """,
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Status check completed",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(type = "boolean")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - requires ADMIN role",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found - account not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping("/{accountNumber}/active")
    public ResponseEntity<Boolean> isAccountActive(
            @Parameter(description = "Account number", required = true)
            @PathVariable @NotBlank String accountNumber) {

        boolean isActive = accountService.isAccountActive(accountNumber);
        return ResponseEntity.ok(isActive);
    }

    // ============================================================
    // ==================== BALANCE OPERATIONS ====================
    // ============================================================

    @Operation(
            summary = "Get account balance by account number",
            description = """
                    Retrieves the balance of a specific account by account number.
                    
                    **Authentication:** Required
                    **Authorization:** Requires ADMIN role
                    
                    **Use case:** When an admin needs to check an account's balance.
                    
                    **Authorization header:** `Authorization: Bearer {token}`
                    """,
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Balance retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(type = "number")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - requires ADMIN role",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found - account not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<BigDecimal> getAccountBalanceByAccountNumber(
            @Parameter(description = "Account number", required = true)
            @PathVariable @NotBlank String accountNumber) {

        BigDecimal balance = accountService.getAccountBalanceByAccountNumber(accountNumber);
        return ResponseEntity.ok(balance);
    }
}
