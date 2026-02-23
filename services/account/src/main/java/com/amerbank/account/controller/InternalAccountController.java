package com.amerbank.account.controller;

import com.amerbank.account.dto.*;
import com.amerbank.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for internal service-to-service account operations.
 * These endpoints are used by other microservices to perform account operations.
 * All endpoints require a valid service JWT token with service scope.
 */
@RestController
@RequestMapping("/account/internal")
@RequiredArgsConstructor
@Validated
@Tag(name = "Internal Service", description = "Internal endpoints for service-to-service communication. Requires service JWT token.")
public class InternalAccountController {

    private final AccountService accountService;

    @Operation(
            summary = "Check account ownership",
            description = """
                    Verifies if a specific account number belongs to a given customer.
                    
                    **Authentication:** Required (service-to-service)
                    **Authorization:** Requires valid service JWT token
                    
                    **Use case:** Used by payment services to verify account ownership before processing transactions.
                    """
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
                    description = "Bad Request - invalid request parameters",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ValidationErrorResponse.class)
                    )
            )
    })
    @PostMapping("/owned")
    public ResponseEntity<Boolean> isAccountOwnedByCustomer(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Request containing customer ID and account number to verify",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ServiceAccountOwnedRequest.class),
                            examples = {
                                    @ExampleObject(value = "{\"customerId\":1,\"accountNumber\":\"550e8400e29b\"}")
                            }
                    )
            )
            @RequestBody ServiceAccountOwnedRequest request) {

        return ResponseEntity.ok(accountService.isAccountOwnedByCustomer(
                request.customerId(), request.accountNumber()));
    }

    @Operation(
            summary = "Deposit funds into account",
            description = """
                    Deposits funds into a customer's account.
                    
                    **Authentication:** Required (service-to-service)
                    **Authorization:** Requires valid service JWT token
                    
                    **Use case:** Used by payment processing services for incoming payments or loan disbursement services for loan amounts.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deposit successful"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - invalid request, account not active, or account not owned by customer",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ValidationErrorResponse.class)
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
    @PostMapping("/deposit")
    public ResponseEntity<Void> performDeposit(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Deposit request with customer ID, account number, and amount",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ServiceDepositBalanceRequest.class),
                            examples = @ExampleObject(value = "{\"customerId\":1,\"accountNumber\":\"550e8400e29b\",\"amount\":500.00}")
                    )
            )
            @Valid @RequestBody ServiceDepositBalanceRequest request) {

        accountService.performDeposit(
                request.customerId(),
                new DepositBalanceRequest(
                        request.accountNumber(),
                        request.amount()
                )
        );
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Perform payment between accounts",
            description = """
                    Transfers funds from one account to another within the bank.
                    
                    **Usage:**
                    - Used by bill payment services for bill settlements
                    - Used by transfer services for internal transfers
                    
                    **Business Rules:**
                    - Both accounts must be ACTIVE
                    - Source account must belong to the specified customer
                    - Source account must have sufficient balance
                    - Source and destination accounts must be different
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment successful"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - insufficient funds, accounts not active, or same accounts",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Account not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/payment")
    public ResponseEntity<Void> performPayment(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Payment request with customer ID, source/destination accounts, and amount",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ServicePaymentRequest.class),
                            examples = @ExampleObject(value = "{\"customerId\":1,\"fromAccountNumber\":\"550e8400e29b\",\"toAccountNumber\":\"550e8400e30c\",\"amount\":100.00}")
                    )
            )
            @Valid @RequestBody ServicePaymentRequest request) {

        accountService.performPayment(
                request.customerId(),
                new PaymentBalanceRequest(
                        request.fromAccountNumber(),
                        request.toAccountNumber(),
                        request.amount()
                )
        );
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Perform refund between accounts",
            description = """
                    Reverses a previous transaction by transferring funds from recipient back to sender.
                    
                    **Usage:**
                    - Used by refund services to process transaction reversals
                    
                    **Business Rules:**
                    - Both accounts must be ACTIVE
                    - Refund initiator must own the source (recipient) account
                    - Source account must have sufficient balance
                    - Source and destination accounts must be different
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Refund successful"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - insufficient funds, accounts not active, or same accounts",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Account not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/refund")
    public ResponseEntity<Void> performRefund(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Refund request with customer ID, source/destination accounts, and amount",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ServiceRefundBalanceRequest.class),
                            examples = @ExampleObject(value = "{\"customerId\":1,\"fromAccountNumber\":\"550e8400e30c\",\"toAccountNumber\":\"550e8400e29b\",\"amount\":50.00}")
                    )
            )
            @Valid @RequestBody ServiceRefundBalanceRequest request) {

        accountService.performRefund(
                request.customerId(),
                new RefundBalanceRequest(
                        request.fromAccountNumber(),
                        request.toAccountNumber(),
                        request.amount()
                )
        );
        return ResponseEntity.ok().build();
    }
}
