package com.amerbank.account.controller;

import com.amerbank.account.dto.*;
import com.amerbank.account.model.AccountType;
import com.amerbank.account.security.JwtUserPrincipal;
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
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST controller for customer-facing account operations.
 * Handles account registration, retrieval, and balance inquiries for authenticated users.
 * All endpoints require JWT authentication and enforce account ownership rules.
 */
@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
@Validated
@Tag(name = "Accounts", description = "Bank account management endpoints for authenticated customers")
public class AccountController {

    private final AccountService accountService;

    private static final String JWT_SCHEME = "Bearer JWT";

    // ============================================================
    // =============== ACCOUNT REGISTRATION & RETRIEVAL ===========
    // ============================================================

    @Operation(
            summary = "Register a new bank account",
            description = """
                    Creates a new bank account for the authenticated customer.
                    
                    **Authentication:** Required
                    **Authorization:** Must be authenticated user
                    
                    **Business Rules:**
                    - Each customer can have only one account per account type (CHECKING, SAVINGS, etc.)
                    - Account is created with an initial balance as configured by the system
                    - Account number is auto-generated and guaranteed to be unique
                    
                    **Use case:** When a new customer wants to open their first account or an existing customer wants to open an additional account type.
                    
                    **Authorization header:** `Authorization: Bearer {token}`
                    """,
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Account successfully created",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Account Created",
                                    value = "Account successfully registered"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - validation failed or account type already exists",
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
                    responseCode = "409",
                    description = "Conflict - account of this type already exists for this customer",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping("/register")
    public ResponseEntity<String> registerAccount(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Account creation request containing the desired account type",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = AccountRequest.class),
                            examples = {
                                    @ExampleObject(name = "Create Checking Account", value = "{\"type\":\"CHECKING\"}"),
                                    @ExampleObject(name = "Create Savings Account", value = "{\"type\":\"SAVINGS\"}")
                            }
                    )
            )
            @Valid @RequestBody AccountRequest accountRequest,
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal) {

        accountService.createAccount(accountRequest, jwtUserPrincipal.customerId());
        return ResponseEntity.ok("Account successfully registered");
    }

    @Operation(
            summary = "Get all accounts for current user",
            description = """
                    Retrieves all bank accounts belonging to the authenticated customer.
                    
                    **Authentication:** Required
                    **Authorization:** Must be authenticated user
                    
                    **Use case:** When a customer wants to see all their accounts.
                    
                    **Authorization header:** `Authorization: Bearer {token}`
                    """,
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Accounts retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AccountInfo.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping("/me")
    public ResponseEntity<List<AccountInfo>> getMyAccounts(
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal) {

        List<AccountInfo> accounts = accountService.getMyAccounts(jwtUserPrincipal.customerId());
        return ResponseEntity.ok(accounts);
    }

    @Operation(
            summary = "Get account by type",
            description = """
                    Retrieves a specific bank account by type for the authenticated customer.
                    
                    **Authentication:** Required
                    **Authorization:** Must be authenticated user
                    
                    **Use case:** When a customer wants to check their CHECKING or SAVINGS account.
                    
                    **Authorization header:** `Authorization: Bearer {token}`
                    """,
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Account found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AccountInfo.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - missing or invalid account type parameter",
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
                    responseCode = "404",
                    description = "Not Found - no account of specified type exists for this customer",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping("/me/type")
    public ResponseEntity<AccountInfo> getMyAccountByType(
            @Parameter(
                    description = "Type of account to retrieve",
                    required = true,
                    schema = @Schema(allowableValues = {"CHECKING", "SAVINGS"})
            )
            @RequestParam @NotNull AccountType type,
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal) {

        AccountInfo account = accountService.getMyAccountByType(jwtUserPrincipal.customerId(), type);
        return ResponseEntity.ok(account);
    }

    @Operation(
            summary = "Verify account ownership",
            description = """
                    Verifies if a specific account number belongs to the authenticated customer.
                    
                    **Authentication:** Required
                    **Authorization:** Must be authenticated user
                    
                    **Use case:** When a customer needs to verify they own a specific account.
                    
                    **Authorization header:** `Authorization: Bearer {token}`
                    """,
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Ownership check completed",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(type = "boolean")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - validation failed",
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
            )
    })
    @PostMapping("/me/owned")
    public ResponseEntity<Boolean> isAccountOwnedByCurrentCustomer(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Request containing the account number to verify",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = MyAccountOwnedRequest.class),
                            examples = @ExampleObject(value = "{\"accountNumber\":\"550e8400e29b\"}")
                    )
            )
            @Valid @RequestBody MyAccountOwnedRequest request,
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal) {

        boolean isOwned = accountService.isAccountOwnedByCustomer(
                jwtUserPrincipal.customerId(),
                request.accountNumber()
        );
        return ResponseEntity.ok(isOwned);
    }

    // ============================================================
    // ==================== BALANCE OPERATIONS ====================
    // ============================================================

    @Operation(
            summary = "Get all account balances",
            description = """
                    Retrieves the balances of all accounts belonging to the authenticated customer.
                    
                    **Authentication:** Required
                    **Authorization:** Must be authenticated user
                    
                    **Use case:** When a customer wants to see the total balance across all their accounts.
                    
                    **Authorization header:** `Authorization: Bearer {token}`
                    """,
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Balances retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AccountBalanceInfo.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping("/me/balances")
    public ResponseEntity<List<AccountBalanceInfo>> getAllAccountsBalances(
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal) {

        List<AccountBalanceInfo> balances = accountService.getAllAccountsBalances(
                jwtUserPrincipal.customerId()
        );
        return ResponseEntity.ok(balances);
    }

    @Operation(
            summary = "Get account balance by type",
            description = """
                    Retrieves the balance of a specific account type for the authenticated customer.
                    
                    **Authentication:** Required
                    **Authorization:** Must be authenticated user
                    
                    **Use case:** When a customer wants to check the balance of their CHECKING or SAVINGS account.
                    
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
                    responseCode = "400",
                    description = "Bad Request - missing or invalid account type parameter",
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
                    responseCode = "404",
                    description = "Not Found - no account of specified type exists for this customer",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping("/me/balance")
    public ResponseEntity<BigDecimal> getMyAccountBalanceByType(
            @Parameter(
                    description = "Type of account",
                    required = true,
                    schema = @Schema(allowableValues = {"CHECKING", "SAVINGS"})
            )
            @RequestParam @NotNull AccountType type,
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal) {

        BigDecimal balance = accountService.getAccountBalance(
                jwtUserPrincipal.customerId(),
                type
        );
        return ResponseEntity.ok(balance);
    }

    @Operation(
            summary = "Check sufficient funds",
            description = """
                    Verifies if the authenticated customer has sufficient funds in a specific account type.
                    
                    **Authentication:** Required
                    **Authorization:** Must be authenticated user
                    
                    **Use case:** Before making a payment or transfer, check if customer has enough balance.
                    
                    **Authorization header:** `Authorization: Bearer {token}`
                    """,
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Funds check completed",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(type = "boolean")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - missing parameters or invalid amount",
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
                    responseCode = "404",
                    description = "Not Found - no account of specified type exists for this customer",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping("/me/has-funds")
    public ResponseEntity<Boolean> hasSufficientFundsByType(
            @Parameter(
                    description = "Type of account",
                    required = true,
                    schema = @Schema(allowableValues = {"CHECKING", "SAVINGS"})
            )
            @RequestParam @NotNull AccountType type,
            @Parameter(
                    description = "Amount to check against balance",
                    required = true,
                    schema = @Schema(minimum = "0.01")
            )
            @RequestParam @NotNull @DecimalMin(value = "0.01", message = "Amount must be greater than zero") BigDecimal amount,
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal) {

        boolean hasFunds = accountService.hasSufficientFundsByType(
                jwtUserPrincipal.customerId(),
                type,
                amount
        );
        return ResponseEntity.ok(hasFunds);
    }
}
