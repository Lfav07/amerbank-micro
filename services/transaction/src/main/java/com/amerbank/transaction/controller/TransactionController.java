package com.amerbank.transaction.controller;

import com.amerbank.transaction.dto.*;
import com.amerbank.transaction.model.TransactionStatus;
import com.amerbank.transaction.model.TransactionType;
import com.amerbank.transaction.security.JwtUserPrincipal;
import com.amerbank.transaction.service.TransactionService;
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
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/me")
    public ResponseEntity<List<TransactionResponse>> getMyTransactions(
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal,
            @RequestParam @NotBlank String accountNumber
    ) {
        Long customerId = jwtUserPrincipal.customerId();
        return ResponseEntity.ok(transactionService.getMyTransactionResponses(customerId, accountNumber));
    }

    @GetMapping("/me/from")
    public ResponseEntity<List<TransactionResponse>> getMyTransactionsByFromAccount(
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal,
            @RequestParam @NotBlank String fromAccountNumber
    ) {
        Long customerId = jwtUserPrincipal.customerId();
        return ResponseEntity.ok(transactionService.getMyTransactionsByFromAccountResponses(customerId, fromAccountNumber));
    }

    @GetMapping("/me/to")
    public ResponseEntity<List<TransactionResponse>> getMyTransactionsByToAccount(
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal,
            @RequestParam @NotBlank String toAccountNumber
    ) {
        Long customerId = jwtUserPrincipal.customerId();
        return ResponseEntity.ok(transactionService.getMyTransactionsByToAccountResponses(customerId, toAccountNumber));
    }

    @GetMapping("/me/transfer")
    public ResponseEntity<List<TransactionResponse>> getMyTransactionsByFromAndToAccount(
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal,
            @RequestParam @NotBlank String fromAccountNumber,
            @RequestParam @NotBlank String toAccountNumber
    ) {
        Long customerId = jwtUserPrincipal.customerId();
        return ResponseEntity.ok(transactionService.getMyTransactionsByFromAndToAccountResponses(customerId, fromAccountNumber, toAccountNumber));
    }

    @GetMapping("/me/status")
    public ResponseEntity<List<TransactionResponse>> getMyTransactionsByStatus(
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal,
            @RequestParam @NotBlank String accountNumber,
            @RequestParam @NotNull TransactionStatus status
    ) {
        Long customerId = jwtUserPrincipal.customerId();
        return ResponseEntity.ok(transactionService.getMyTransactionsByStatusResponses(customerId, accountNumber, status));
    }

    @GetMapping("/me/type")
    public ResponseEntity<List<TransactionResponse>> getMyTransactionsByType(
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal,
            @RequestParam @NotBlank String accountNumber,
            @RequestParam @NotNull TransactionType type
    ) {
        Long customerId = jwtUserPrincipal.customerId();
        return ResponseEntity.ok(transactionService.getMyTransactionsByTypeResponses(customerId, accountNumber, type));
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> createDeposit(
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal,
            @RequestHeader("idempotency-key") @NotBlank String idempotencyKey,
            @Valid @RequestBody DepositTransactionRequest request
    ) {
        Long customerId = jwtUserPrincipal.customerId();
        TransactionResponse response = transactionService.createDepositTransaction(customerId, idempotencyKey, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/payment")
    public ResponseEntity<TransactionResponse> createPayment(
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal,
            @RequestHeader("idempotency-key") @NotBlank String idempotencyKey,
            @Valid @RequestBody PaymentTransactionRequest request
    ) {
        Long customerId = jwtUserPrincipal.customerId();
        TransactionResponse response = transactionService.createPaymentTransaction(customerId, idempotencyKey, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refund")
    public ResponseEntity<TransactionResponse> createRefund(
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal,
            @RequestHeader("idempotency-key") @NotBlank String idempotencyKey,
            @Valid @RequestBody RefundTransactionRequest request
    ) {
        Long customerId = jwtUserPrincipal.customerId();
        TransactionResponse response = transactionService.createRefundTransaction(customerId, idempotencyKey, request);
        return ResponseEntity.ok(response);
    }
}
