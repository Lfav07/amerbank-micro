package com.amerbank.transaction.controller;

import com.amerbank.transaction.dto.ErrorResponse;
import com.amerbank.transaction.dto.TransactionResponse;
import com.amerbank.transaction.model.TransactionStatus;
import com.amerbank.transaction.model.TransactionType;
import com.amerbank.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transaction/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Management", description = "Administrative endpoints for transaction management. Requires ADMIN role.")
public class TransactionAdminController {

    private final TransactionService transactionService;

    @Operation(
            summary = "Get transaction by ID",
            description = "Retrieves a transaction by its unique identifier. Requires ADMIN role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction found",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Transaction not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer JWT")
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransactionById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.getTransactionResponseById(id));
    }

    // Only fromAccount
    @Operation(
            summary = "Get transactions by source account",
            description = "Retrieves all transactions for a specific source account. Requires ADMIN role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer JWT")
    @GetMapping(params = {"fromAccount", "!toAccount", "!status", "!type"})
    public ResponseEntity<List<TransactionResponse>> getByFromAccount(
            @RequestParam String fromAccount) {
        return ResponseEntity.ok(
                transactionService.getTransactionResponsesByFromAccountNumber(fromAccount));
    }

    // Only toAccount
    @Operation(
            summary = "Get transactions by destination account",
            description = "Retrieves all transactions for a specific destination account. Requires ADMIN role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer JWT")
    @GetMapping(params = {"toAccount", "!fromAccount", "!status", "!type"})
    public ResponseEntity<List<TransactionResponse>> getByToAccount(
            @RequestParam String toAccount) {
        return ResponseEntity.ok(
                transactionService.getTransactionResponsesByToAccountNumber(toAccount));
    }

    // from + to
    @Operation(
            summary = "Get transactions between accounts",
            description = "Retrieves all transactions between two specific accounts. Requires ADMIN role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer JWT")
    @GetMapping(params = {"fromAccount", "toAccount", "!status", "!type"})
    public ResponseEntity<List<TransactionResponse>> getByFromAndToAccount(
            @RequestParam String fromAccount,
            @RequestParam String toAccount) {
        return ResponseEntity.ok(
                transactionService.getTransactionResponsesByFromAndToAccountNumber(fromAccount, toAccount));
    }

    // status only
    @Operation(
            summary = "Get transactions by status",
            description = "Retrieves all transactions with a specific status. Requires ADMIN role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer JWT")
    @GetMapping(params = {"status", "!fromAccount", "!toAccount", "!type"})
    public ResponseEntity<List<TransactionResponse>> getByStatus(
            @RequestParam TransactionStatus status) {
        return ResponseEntity.ok(
                transactionService.getTransactionResponsesByStatus(status));
    }

    // type only
    @Operation(
            summary = "Get transactions by type",
            description = "Retrieves all transactions of a specific type. Requires ADMIN role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer JWT")
    @GetMapping(params = {"type", "!fromAccount", "!toAccount", "!status"})
    public ResponseEntity<List<TransactionResponse>> getByType(
            @RequestParam TransactionType type) {
        return ResponseEntity.ok(
                transactionService.getTransactionResponsesByType(type));
    }
}
