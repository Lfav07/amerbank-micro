package com.amerbank.transaction.controller;

import com.amerbank.transaction.dto.*;
import com.amerbank.transaction.model.Transaction;
import com.amerbank.transaction.model.TransactionStatus;
import com.amerbank.transaction.model.TransactionType;
import com.amerbank.transaction.security.JwtUserPrincipal;
import com.amerbank.transaction.service.TransactionMapper;
import com.amerbank.transaction.service.TransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
@AllArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    String extractJwt(String authorization){
        return authorization.replace("Bearer ", "");
    }

    // --- GET TRANSACTIONS ---

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable UUID id) {
        Transaction transaction = transactionService.findTransactionById(id);
        return ResponseEntity.ok(transactionMapper.toResponse(transaction));
    }

    @GetMapping("/from-account/{fromAccount}")
    public ResponseEntity<List<TransactionResponse>> getByFromAccount(@PathVariable String fromAccount) {
        return ResponseEntity.ok(
                transactionService.findTransactionsByFromAccountNumber(fromAccount)
                        .stream().map(transactionMapper::toResponse).toList()
        );
    }

    @GetMapping(params = {"fromAccount", "toAccount"})
    public ResponseEntity<List<TransactionResponse>> getByFromAndToAccount(
            @RequestParam String fromAccount,
            @RequestParam String toAccount) {
        List<TransactionResponse> response = transactionService
                .findTransactionsByFromAndToAccountNumber(fromAccount, toAccount)
                .stream()
                .map(transactionMapper::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }


    @GetMapping("/from/{fromAccount}/to/{toAccount}")
    public ResponseEntity<List<TransactionResponse>> getByFromAndTo(
            @PathVariable String fromAccount,
            @PathVariable String toAccount
    ) {
        List<Transaction> list = transactionService.findTransactionsByFromAndToAccountNumber(fromAccount, toAccount);
        return ResponseEntity.ok(
                list.stream().map(transactionMapper::toResponse).toList()
        );
    }


    @GetMapping("/to-account/{toAccount}")
    public ResponseEntity<List<TransactionResponse>> getByToAccount(@PathVariable String toAccount) {
        return ResponseEntity.ok(
                transactionService.findTransactionsByToAccountNumber(toAccount)
                        .stream().map(transactionMapper::toResponse).toList()
        );
    }


    @GetMapping("/status/{status}")
    public ResponseEntity<List<TransactionResponse>> getByStatus(@PathVariable TransactionStatus status) {
        return ResponseEntity.ok(
                transactionService.findTransactionsByStatus(status)
                        .stream().map(transactionMapper::toResponse).toList()
        );
    }


    @GetMapping("/type/{type}")
    public ResponseEntity<List<TransactionResponse>> getByType(@PathVariable TransactionType type) {
        return ResponseEntity.ok(
                transactionService.findTransactionsByType(type)
                        .stream().map(transactionMapper::toResponse).toList()
        );
    }


    @GetMapping("/account/{accountNumber}/me")
    public ResponseEntity<List<TransactionResponse>> getMyTransactions(
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal,
            @PathVariable String accountNumber
    ) {
       Long customerId = jwtUserPrincipal.customerId();
        return ResponseEntity.ok(
                transactionService.getMyTransactions(customerId, accountNumber)
                        .stream().map(transactionMapper::toResponse).toList()
        );
    }
    // --- CREATE TRANSACTIONS ---

    @PostMapping("/user/deposit")
    public ResponseEntity<TransactionResponse> createDeposit(
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal,
            @RequestHeader("idempotency-key") @NotBlank String idempotencyKey,
            @Valid @RequestBody DepositTransactionRequest request
    ) {
        Long customerId = jwtUserPrincipal.customerId();
        TransactionResponse response = transactionService.createDepositTransaction(customerId, idempotencyKey, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/user/payment")
    public ResponseEntity<TransactionResponse> createPayment(
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal,
            @RequestHeader("idempotency-key") @NotBlank String idempotencyKey,
            @Valid @RequestBody PaymentTransactionRequest request
    ) {
        Long customerId = jwtUserPrincipal.customerId();
        TransactionResponse response = transactionService.createPaymentTransaction(customerId, idempotencyKey, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/user/refund")
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
