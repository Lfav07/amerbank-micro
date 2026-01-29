package com.amerbank.account.controller;

import com.amerbank.account.dto.*;
import com.amerbank.account.model.AccountType;
import com.amerbank.account.service.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST controller for administrative account operations.
 * Handles account management, status updates, and administrative queries.
 * All endpoints require ADMIN role as configured in SecurityConfig.
 */
@RestController
@RequestMapping("/account/admin")
@RequiredArgsConstructor
@Validated
public class AccountAdminController {

    private final AccountService accountService;

    // ============================================================
    // ================= ACCOUNT RETRIEVAL ========================
    // ============================================================

    /**
     * Retrieves all accounts for a specific customer.
     *
     * @param customerId the customer ID
     * @return list of all accounts for the customer
     */
    @GetMapping("/customers/{customerId}")
    public ResponseEntity<List<AccountResponse>> getAccountsByCustomerId(
            @PathVariable @NotNull Long customerId) {

        List<AccountResponse> accounts = accountService.getAccountsByCustomerId(customerId);
        return ResponseEntity.ok(accounts);
    }

    /**
     * Retrieves a specific account for a customer by account type.
     *
     * @param customerId the customer ID
     * @param type       the account type
     * @return the matching account
     */
    @GetMapping("/customers/{customerId}/type")
    public ResponseEntity<AccountResponse> getAccountByCustomerIdAndType(
            @PathVariable @NotNull Long customerId,
            @RequestParam @NotNull AccountType type) {

        AccountResponse account = accountService.getAccountByCustomerIdAndType(customerId, type);
        return ResponseEntity.ok(account);
    }

    /**
     * Retrieves account details by account number.
     *
     * @param accountNumber the account number
     * @return account information
     */
    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountByAccountNumber(
            @PathVariable @NotBlank String accountNumber) {

        AccountResponse account = accountService.getAccountByAccountNumber(accountNumber);
        return ResponseEntity.ok(account);
    }

    // ============================================================
    // ================= ACCOUNT MANAGEMENT =======================
    // ============================================================

    /**
     * Updates the type of an account.
     *
     * @param accountNumber the account number
     * @param request       the account type update request
     * @return the updated account information
     */
    @PutMapping("/{accountNumber}/type")
    public ResponseEntity<AccountResponse> updateAccountType(
            @PathVariable @NotBlank String accountNumber,
            @Valid @RequestBody AccountUpdateTypeRequest request) {

        AccountResponse updatedAccount = accountService.updateAccountType(accountNumber, request);
        return ResponseEntity.ok(updatedAccount);
    }

    /**
     * Updates the status of an account.
     *
     * @param accountNumber the account number
     * @param request       the account status update request
     * @return the updated account information
     */
    @PutMapping("/{accountNumber}/status")
    public ResponseEntity<AccountResponse> updateAccountStatus(
            @PathVariable @NotBlank String accountNumber,
            @Valid @RequestBody AccountUpdateStatusRequest request) {

        AccountResponse updatedAccount = accountService.updateAccountStatus(accountNumber, request);
        return ResponseEntity.ok(updatedAccount);
    }

    /**
     * Suspends a specific account.
     *
     * @param accountNumber the account number
     * @return the updated account information after suspension
     */
    @PutMapping("/{accountNumber}/suspend")
    public ResponseEntity<AccountResponse> suspendAccount(
            @PathVariable @NotBlank String accountNumber) {

        AccountResponse suspendedAccount = accountService.suspendAccount(accountNumber);
        return ResponseEntity.ok(suspendedAccount);
    }

    /**
     * Deletes an account by its account number.
     * Requires customer ID for verification.
     *
     * @param accountNumber the account number to delete
     * @param customerId    the customer ID for verification
     * @return 204 No Content if deletion is successful
     */
    @DeleteMapping("/{accountNumber}")
    public ResponseEntity<Void> deleteAccount(
            @PathVariable @NotBlank String accountNumber,
            @RequestParam @NotNull Long customerId) {

        accountService.deleteAccount(accountNumber, customerId);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // ============= ACCOUNT STATUS & OWNERSHIP CHECKS ============
    // ============================================================

    /**
     * Checks if an account is currently active.
     *
     * @param accountNumber the account number
     * @return true if the account is active, false otherwise
     */
    @GetMapping("/{accountNumber}/active")
    public ResponseEntity<Boolean> isAccountActive(
            @PathVariable @NotBlank String accountNumber) {

        boolean isActive = accountService.isAccountActive(accountNumber);
        return ResponseEntity.ok(isActive);
    }

    // ============================================================
    // ==================== BALANCE OPERATIONS ====================
    // ============================================================

    /**
     * Retrieves the balance of a specific account by account number.
     *
     * @param accountNumber the account number
     * @return the account balance
     */
    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<BigDecimal> getAccountBalanceByAccountNumber(
            @PathVariable @NotBlank String accountNumber) {

        BigDecimal balance = accountService.getAccountBalanceByAccountNumber(accountNumber);
        return ResponseEntity.ok(balance);
    }
}