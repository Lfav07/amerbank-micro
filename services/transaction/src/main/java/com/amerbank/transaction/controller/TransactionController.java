package com.amerbank.transaction.controller;

import com.amerbank.transaction.dto.*;
import com.amerbank.transaction.model.TransactionStatus;
import com.amerbank.transaction.model.TransactionType;
import com.amerbank.transaction.security.JwtUserPrincipal;
import com.amerbank.transaction.service.TransactionService;
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
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transaction")
@AllArgsConstructor
@Tag(name = "Transactions", description = "Financial transaction operations including deposits, payments, and refunds")
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(
            summary = "Get account transactions",
            description = "Retrieves all transactions (both incoming and outgoing) for a specific account owned by the authenticated user. " +
                    "This includes transactions where the account serves as either the source or destination account."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Transactions retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TransactionResponse.class),
                            examples = @ExampleObject(
                                    name = "Transaction List Response",
                                    value = "[{\"id\":\"550e8400-e29b-41d4-a716-446655440000\",\"amount\":100.00,\"fromAccountNumber\":\"550e8400-e29b-41d4-a716-446655440001\",\"toAccountNumber\":\"550e8400-e29b-41d4-a716-446655440002\",\"type\":\"PAYMENT\",\"status\":\"COMPLETED\",\"createdAt\":\"2024-01-15T10:30:00\"}]"
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - account does not belong to user",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer JWT")
    @GetMapping("/me")
    public ResponseEntity<List<TransactionResponse>> getMyTransactions(
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal,
            @Parameter(description = "Account number to retrieve transactions for", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam @NotBlank String accountNumber
    ) {
        Long customerId = jwtUserPrincipal.customerId();
        return ResponseEntity.ok(transactionService.getMyTransactionResponses(customerId, accountNumber));
    }

    @Operation(
            summary = "Get outgoing transactions",
            description = "Retrieves all transactions where the authenticated user's account is the source (money sent out). " +
                    "This includes payments and other debits from the account."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - account does not belong to user",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer JWT")
    @GetMapping("/me/from")
    public ResponseEntity<List<TransactionResponse>> getMyTransactionsByFromAccount(
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal,
            @Parameter(description = "Source account number", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam @NotBlank String fromAccountNumber
    ) {
        Long customerId = jwtUserPrincipal.customerId();
        return ResponseEntity.ok(transactionService.getMyTransactionsByFromAccountResponses(customerId, fromAccountNumber));
    }

    @Operation(
            summary = "Get incoming transactions",
            description = "Retrieves all transactions where the authenticated user's account is the destination (money received). " +
                    "This includes deposits, payments received, and refunds."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - account does not belong to user",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer JWT")
    @GetMapping("/me/to")
    public ResponseEntity<List<TransactionResponse>> getMyTransactionsByToAccount(
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal,
            @Parameter(description = "Destination account number", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam @NotBlank String toAccountNumber
    ) {
        Long customerId = jwtUserPrincipal.customerId();
        return ResponseEntity.ok(transactionService.getMyTransactionsByToAccountResponses(customerId, toAccountNumber));
    }

    @Operation(
            summary = "Get transfer history between accounts",
            description = "Retrieves all transactions between two specific accounts where the authenticated user's account is the source."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - source account does not belong to user",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer JWT")
    @GetMapping("/me/transfer")
    public ResponseEntity<List<TransactionResponse>> getMyTransactionsByFromAndToAccount(
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal,
            @Parameter(description = "Source account number", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam @NotBlank String fromAccountNumber,
            @Parameter(description = "Destination account number", example = "550e8400-e29b-41d4-a716-446655440001")
            @RequestParam @NotBlank String toAccountNumber
    ) {
        Long customerId = jwtUserPrincipal.customerId();
        return ResponseEntity.ok(transactionService.getMyTransactionsByFromAndToAccountResponses(customerId, fromAccountNumber, toAccountNumber));
    }

    @Operation(
            summary = "Get transactions by status",
            description = "Retrieves all outgoing transactions for a specific account filtered by transaction status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - account does not belong to user",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer JWT")
    @GetMapping("/me/status")
    public ResponseEntity<List<TransactionResponse>> getMyTransactionsByStatus(
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal,
            @Parameter(description = "Account number", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam @NotBlank String accountNumber,
            @Parameter(description = "Transaction status to filter by", example = "COMPLETED")
            @RequestParam @NotNull TransactionStatus status
    ) {
        Long customerId = jwtUserPrincipal.customerId();
        return ResponseEntity.ok(transactionService.getMyTransactionsByStatusResponses(customerId, accountNumber, status));
    }

    @Operation(
            summary = "Get transactions by type",
            description = "Retrieves all outgoing transactions for a specific account filtered by transaction type."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - account does not belong to user",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer JWT")
    @GetMapping("/me/type")
    public ResponseEntity<List<TransactionResponse>> getMyTransactionsByType(
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal,
            @Parameter(description = "Account number", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam @NotBlank String accountNumber,
            @Parameter(description = "Transaction type to filter by", example = "PAYMENT")
            @RequestParam @NotNull TransactionType type
    ) {
        Long customerId = jwtUserPrincipal.customerId();
        return ResponseEntity.ok(transactionService.getMyTransactionsByTypeResponses(customerId, accountNumber, type));
    }

    @Operation(
            summary = "Create deposit",
            description = """
                    Creates a deposit transaction to add funds to an account.
                    
                    **Business Rules:**
                    - The destination account must exist and be active
                    - Only the account owner can initiate deposits
                    - The deposit amount must be positive
                    
                    **Atomicity:**
                    This operation is atomic - either the deposit succeeds and the balance is updated, 
                    or the entire operation fails with no partial state changes.
                    
                    **Idempotency:**
                    Requires an idempotency key header to prevent duplicate deposits in case of network failures.
                    The idempotency key must be unique for each request - use a different key after each request.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Deposit created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TransactionResponse.class),
                            examples = @ExampleObject(
                                    name = "Successful Deposit",
                                    value = "{\"id\":\"550e8400-e29b-41d4-a716-446655440000\",\"amount\":500.00,\"fromAccountNumber\":\"550e8400-e29b-41d4-a716-446655440001\",\"toAccountNumber\":\"550e8400-e29b-41d4-a716-446655440002\",\"type\":\"DEPOSIT\",\"status\":\"COMPLETED\",\"createdAt\":\"2024-01-15T10:30:00\"}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request - validation error",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - account does not belong to user",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Account Not Found",
                                    value = "{\"timestamp\":\"2024-01-15T10:30:00\",\"status\":404,\"error\":\"Account Not Found\",\"message\":\"Account not found\",\"path\":\"/transaction/deposit\",\"traceId\":\"abc-123\"}"
                            )
                    ))
    })
    @SecurityRequirement(name = "Bearer JWT")
    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> createDeposit(
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal,
            @Parameter(description = "Idempotency key to prevent duplicate deposits", example = "dep-123456")
            @RequestHeader("idempotency-key") @NotBlank String idempotencyKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Deposit transaction details",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DepositTransactionRequest.class),
                            examples = {
                                    @ExampleObject(name = "Deposit Request", value = "{\"amount\":500.00,\"description\":\"Salary deposit\",\"fromAccountNumber\":\"550e8400-e29b-41d4-a716-446655440001\",\"toAccountNumber\":\"550e8400-e29b-41d4-a716-446655440002\"}")
                            }
                    )
            )
            @Valid @RequestBody DepositTransactionRequest request
    ) {
        Long customerId = jwtUserPrincipal.customerId();
        TransactionResponse response = transactionService.createDepositTransaction(customerId, idempotencyKey, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Create payment (transfer)",
            description = """
                    Creates a payment transaction to transfer funds between accounts.
                    
                    **Debit/Credit Logic:**
                    - Source account is DEBITED (money leaves the account)
                    - Destination account is CREDITED (money enters the account)
                    
                    **Business Rules:**
                    - Both source and destination accounts must exist and be active
                    - Only the source account owner can initiate the payment
                    - Source account must have sufficient funds (balance >= amount)
                    - Cannot transfer to the same account
                    
                    **Atomicity:**
                    This operation is atomic - either both the debit and credit succeed, 
                    or the entire operation fails with no partial state changes. 
                    If insufficient funds, no balance changes occur.
                    
                    **Idempotency:**
                    Requires an idempotency key header to prevent duplicate payments in case of network failures.
                    The idempotency key must be unique for each request - use a different key after each request.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Payment created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TransactionResponse.class),
                            examples = @ExampleObject(
                                    name = "Successful Payment",
                                    value = "{\"id\":\"550e8400-e29b-41d4-a716-446655440000\",\"amount\":100.00,\"fromAccountNumber\":\"550e8400-e29b-41d4-a716-446655440001\",\"toAccountNumber\":\"550e8400-e29b-41d4-a716-446655440002\",\"type\":\"PAYMENT\",\"status\":\"COMPLETED\",\"createdAt\":\"2024-01-15T10:30:00\"}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request - validation error",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - source account does not belong to user",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict - insufficient funds",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Insufficient Funds",
                                    value = "{\"timestamp\":\"2024-01-15T10:30:00\",\"status\":409,\"error\":\"Conflict\",\"message\":\"Insufficient funds in source account\",\"path\":\"/transaction/payment\",\"traceId\":\"abc-123\"}"
                            )
                    )
            )
    })
    @SecurityRequirement(name = "Bearer JWT")
    @PostMapping("/payment")
    public ResponseEntity<TransactionResponse> createPayment(
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal,
            @Parameter(description = "Idempotency key to prevent duplicate payments", example = "pay-123456")
            @RequestHeader("idempotency-key") @NotBlank String idempotencyKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Payment (transfer) transaction details. The 'fromAccountNumber' will be debited and 'toAccountNumber' will be credited.",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PaymentTransactionRequest.class),
                            examples = {
                                    @ExampleObject(name = "Payment Request", value = "{\"amount\":100.00,\"description\":\"Bill payment\",\"fromAccountNumber\":\"550e8400-e29b-41d4-a716-446655440001\",\"toAccountNumber\":\"550e8400-e29b-41d4-a716-446655440002\"}")
                            }
                    )
            )
            @Valid @RequestBody PaymentTransactionRequest request
    ) {
        Long customerId = jwtUserPrincipal.customerId();
        TransactionResponse response = transactionService.createPaymentTransaction(customerId, idempotencyKey, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Create refund",
            description = """
                    Creates a refund transaction to reverse a previous payment.
                    
                    **Debit/Credit Logic:**
                    - The original transaction's destination account is DEBITED (money leaves)
                    - The original transaction's source account is CREDITED (money returns)
                    
                    **Business Rules:**
                    - Original transaction must exist and not already be refunded
                    - Only the owner of the original destination account can initiate refund
                    - The refund amount equals the original transaction amount
                    
                    **Atomicity:**
                    This operation is atomic - the original transaction is marked as REVERSED 
                    and the refund transfer succeeds or fails as a unit.
                    
                    **Idempotency:**
                    Requires an idempotency key header to prevent duplicate refunds.
                    The idempotency key must be unique for each request - use a different key after each request.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Refund created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TransactionResponse.class),
                            examples = @ExampleObject(
                                    name = "Successful Refund",
                                    value = "{\"id\":\"550e8400-e29b-41d4-a716-446655440003\",\"amount\":100.00,\"fromAccountNumber\":\"550e8400-e29b-41d4-a716-446655440002\",\"toAccountNumber\":\"550e8400-e29b-41d4-a716-446655440001\",\"type\":\"REFUND\",\"status\":\"COMPLETED\",\"createdAt\":\"2024-01-15T11:00:00\"}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request - validation error",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - only destination account owner can refund",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Original transaction not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - transaction already refunded",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Already Refunded",
                                    value = "{\"timestamp\":\"2024-01-15T10:30:00\",\"status\":409,\"error\":\"Conflict\",\"message\":\"Transaction already refunded\",\"path\":\"/transaction/refund\",\"traceId\":\"abc-123\"}"
                            )
                    )
            )
    })
    @SecurityRequirement(name = "Bearer JWT")
    @PostMapping("/refund")
    public ResponseEntity<TransactionResponse> createRefund(
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal,
            @Parameter(description = "Idempotency key to prevent duplicate refunds", example = "ref-123456")
            @RequestHeader("idempotency-key") @NotBlank String idempotencyKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Refund request containing the ID of the transaction to refund",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RefundTransactionRequest.class),
                            examples = {
                                    @ExampleObject(name = "Refund Request", value = "{\"transactionId\":\"550e8400-e29b-41d4-a716-446655440000\"}")
                            }
                    )
            )
            @Valid @RequestBody RefundTransactionRequest request
    ) {
        Long customerId = jwtUserPrincipal.customerId();
        TransactionResponse response = transactionService.createRefundTransaction(customerId, idempotencyKey, request);
        return ResponseEntity.ok(response);
    }
}
