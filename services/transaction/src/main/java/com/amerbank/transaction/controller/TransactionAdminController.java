package com.amerbank.transaction.controller;

import com.amerbank.transaction.dto.TransactionResponse;
import com.amerbank.transaction.model.TransactionStatus;
import com.amerbank.transaction.model.TransactionType;
import com.amerbank.transaction.service.TransactionService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable @NotNull UUID id) {
        return ResponseEntity.ok(transactionService.getTransactionResponseById(id));
    }

    @GetMapping(params = "fromAccount")
    public ResponseEntity<List<TransactionResponse>> getByFromAccount(@RequestParam @NotBlank String fromAccount) {
        return ResponseEntity.ok(transactionService.getTransactionResponsesByFromAccountNumber(fromAccount));
    }

    @GetMapping(params = "toAccount")
    public ResponseEntity<List<TransactionResponse>> getByToAccount(@RequestParam @NotBlank String toAccount) {
        return ResponseEntity.ok(transactionService.getTransactionResponsesByToAccountNumber(toAccount));
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getByFromAndToAccount(
            @RequestParam @NotBlank String fromAccount,
            @RequestParam @NotBlank String toAccount) {
        return ResponseEntity.ok(transactionService.getTransactionResponsesByFromAndToAccountNumber(fromAccount, toAccount));
    }

    @GetMapping(params = "status")
    public ResponseEntity<List<TransactionResponse>> getByStatus(@RequestParam @NotNull TransactionStatus status) {
        return ResponseEntity.ok(transactionService.getTransactionResponsesByStatus(status));
    }

    @GetMapping(params = "type")
    public ResponseEntity<List<TransactionResponse>> getByType(@RequestParam @NotNull TransactionType type) {
        return ResponseEntity.ok(transactionService.getTransactionResponsesByType(type));
    }

}
