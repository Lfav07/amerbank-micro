package com.amerbank.transaction.controller;

import com.amerbank.transaction.dto.*;
import com.amerbank.transaction.model.Transaction;
import com.amerbank.transaction.model.TransactionStatus;
import com.amerbank.transaction.model.TransactionType;
import com.amerbank.transaction.service.TransactionMapper;
import com.amerbank.transaction.service.TransactionService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
@AllArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private  final TransactionMapper transactionMapper;

    // --- GET TRANSACTIONS ---

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable UUID id) {
        Transaction transaction = transactionService.findTransactionById(id);
        return ResponseEntity.ok(transactionMapper.toResponse(transaction));
    }

    @GetMapping("/from/{fromAccount}")
    public ResponseEntity<List<Transaction>> getTransactionsByFromAccount(@PathVariable String fromAccount) {
        return ResponseEntity.ok(transactionService.findTransactionsByFromAccountNumber(fromAccount));
    }

    @GetMapping("/to/{toAccount}")
    public ResponseEntity<List<Transaction>> getTransactionsByToAccount(@PathVariable String toAccount) {
        return ResponseEntity.ok(transactionService.findTransactionsByToAccountNumber(toAccount));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Transaction>> getTransactionsByStatus(@PathVariable TransactionStatus status) {
        return ResponseEntity.ok(transactionService.findTransactionsByStatus(status));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<Transaction>> getTransactionsByType(@PathVariable TransactionType type) {
        return ResponseEntity.ok(transactionService.findTransactionsByType(type));
    }

    @GetMapping("/my/{accountNumber}")
    public ResponseEntity<List<Transaction>> getMyTransactions(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String accountNumber
    ) {


        String jwtToken = authorization.substring(7);

        return ResponseEntity.ok(transactionService.getMyTransactions(jwtToken, accountNumber));
    }

    // --- CREATE TRANSACTIONS ---

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> createDeposit(
            @RequestHeader("Authorization") String authorization,
            @RequestBody DepositTransactionRequest request
    ) {
        String jwtToken = authorization.replace("Bearer ", "");
        TransactionResponse response = transactionService.createDepositTransaction(jwtToken, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/payment")
    public ResponseEntity<TransactionResponse> createPayment(
            @RequestHeader("Authorization") String authorization,
            @RequestBody PaymentTransactionRequest request
    ) {
        String jwtToken = authorization.replace("Bearer ", "");
        TransactionResponse response = transactionService.createPaymentTransaction(jwtToken, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refund")
    public ResponseEntity<TransactionResponse> createRefund(
            @RequestHeader("Authorization") String authorization,
            @RequestBody RefundTransactionRequest request
    ) {
        String jwtToken = authorization.replace("Bearer ", "");
        TransactionResponse response = transactionService.createRefundTransaction(jwtToken, request);
        return ResponseEntity.ok(response);
    }
}
