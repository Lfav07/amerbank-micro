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
            @RequestHeader("Authorization") String authorization,
            @PathVariable String accountNumber
    ) {
        String jwt = authorization.substring(7);
        return ResponseEntity.ok(
                transactionService.getMyTransactions(jwt, accountNumber)
                        .stream().map(transactionMapper::toResponse).toList()
        );
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
