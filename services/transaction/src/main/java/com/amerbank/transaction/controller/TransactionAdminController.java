package com.amerbank.transaction.controller;

import com.amerbank.transaction.dto.TransactionResponse;
import com.amerbank.transaction.model.TransactionStatus;
import com.amerbank.transaction.model.TransactionType;
import com.amerbank.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transaction/admin")
@RequiredArgsConstructor
public class TransactionAdminController {

    private final TransactionService transactionService;

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransactionById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.getTransactionResponseById(id));
    }

    // Only fromAccount
    @GetMapping(params = {"fromAccount", "!toAccount", "!status", "!type"})
    public ResponseEntity<List<TransactionResponse>> getByFromAccount(
            @RequestParam String fromAccount) {
        return ResponseEntity.ok(
                transactionService.getTransactionResponsesByFromAccountNumber(fromAccount));
    }

    // Only toAccount
    @GetMapping(params = {"toAccount", "!fromAccount", "!status", "!type"})
    public ResponseEntity<List<TransactionResponse>> getByToAccount(
            @RequestParam String toAccount) {
        return ResponseEntity.ok(
                transactionService.getTransactionResponsesByToAccountNumber(toAccount));
    }

    // from + to
    @GetMapping(params = {"fromAccount", "toAccount", "!status", "!type"})
    public ResponseEntity<List<TransactionResponse>> getByFromAndToAccount(
            @RequestParam String fromAccount,
            @RequestParam String toAccount) {
        return ResponseEntity.ok(
                transactionService.getTransactionResponsesByFromAndToAccountNumber(fromAccount, toAccount));
    }

    // status only
    @GetMapping(params = {"status", "!fromAccount", "!toAccount", "!type"})
    public ResponseEntity<List<TransactionResponse>> getByStatus(
            @RequestParam TransactionStatus status) {
        return ResponseEntity.ok(
                transactionService.getTransactionResponsesByStatus(status));
    }

    // type only
    @GetMapping(params = {"type", "!fromAccount", "!toAccount", "!status"})
    public ResponseEntity<List<TransactionResponse>> getByType(
            @RequestParam TransactionType type) {
        return ResponseEntity.ok(
                transactionService.getTransactionResponsesByType(type));
    }
}
