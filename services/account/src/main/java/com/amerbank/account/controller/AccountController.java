package com.amerbank.account.controller;

import com.amerbank.account.dto.*;
import com.amerbank.account.model.AccountType;
import com.amerbank.account.security.JwtUserPrincipal;
import com.amerbank.account.service.AccountService;
import com.amerbank.common_dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST controller for managing account-related operations.
 */
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    // ============================================================
    // =============== ACCOUNT REGISTRATION & RETRIEVAL ============
    // ============================================================

    /**
     * Registers a new account for the authenticated user.
     *
     * @param accountRequest   the account creation data
     * @param jwtUserPrincipal the authenticated customer's data
     * @return a success message or an error response
     */
    @PostMapping
    public ResponseEntity<?> registerAccount(
            @RequestBody AccountRequest accountRequest,
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal) {

        accountService.createAccount(accountRequest, jwtUserPrincipal.customerId());
        return ResponseEntity.ok("Account successfully registered");
    }

    /**
     * Retrieves the authenticated user's accounts.
     *
     * @param jwtUserPrincipal the authenticated customer's data
     * @return list of account info
     */
    @GetMapping("/me")
    public ResponseEntity<List<AccountInfo>> getMyAccounts(
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal) {

        List<AccountInfo> accounts = accountService.getMyAccounts(jwtUserPrincipal.customerId());
        return ResponseEntity.ok(accounts);
    }

    /**
     * Retrieves all accounts for a specific customer.
     *
     * @param customerId the customer ID
     * @return list of all accounts
     */
    @GetMapping("/customers/{customerId}")
    public ResponseEntity<List<AccountResponse>> getAccountsByCustomerId(
            @PathVariable Long customerId) {

        return ResponseEntity.ok(accountService.getAccountsByCustomerId(customerId));
    }

    /**
     * Retrieves account(s) by customer ID and optionally filters by type.
     *
     * @param customerId the customer ID
     * @param type       the account type (optional)
     * @return list of matching accounts
     */
    @GetMapping("/customers/{customerId}/filter")
    public ResponseEntity<List<AccountResponse>> getAccountsByCustomerIdAndType(
            @PathVariable Long customerId,
            @RequestParam(required = false) AccountType type) {

        List<AccountResponse> accounts = (type != null)
                ? List.of(accountService.getAccountByCustomerIdAndType(customerId, type))
                : accountService.getAccountsByCustomerId(customerId);

        return ResponseEntity.ok(accounts);
    }

    /**
     * Retrieves account details by account number.
     *
     * @param accountNumber the account number
     * @return account response with account information
     */
    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountByAccountNumber(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccountByAccountNumber(accountNumber));
    }

    // ============================================================
    // ================= ACCOUNT MANAGEMENT =======================
    // ============================================================

    /**
     * Updates the type of an account.
     *
     * @param request the account update request
     * @return the updated account information
     */
    @PutMapping("/{accountNumber}/type")
    public ResponseEntity<AccountResponse> updateAccountType(@RequestBody AccountUpdateTypeRequest request) {
        return ResponseEntity.ok(accountService.updateAccountType(request));
    }

    /**
     * Updates the status of an account.
     *
     * @param request the account update request
     * @return the updated account information
     */
    @PutMapping("/{accountNumber}/status")
    public ResponseEntity<AccountResponse> updateAccountStatus(@RequestBody AccountUpdateStatusRequest request) {
        return ResponseEntity.ok(accountService.updateAccountStatus(request));
    }

    /**
     * Suspends a specific account.
     *
     * @param accountNumber the account number
     * @return the updated account information after suspension
     */
    @PutMapping("/{accountNumber}/suspend")
    public ResponseEntity<AccountResponse> suspendAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.suspendAccount(accountNumber));
    }

    /**
     * Deletes an account by its account number.
     *
     * @param accountNumber the account number
     * @return a 204 No Content response if deletion is successful
     */
    @DeleteMapping("/{accountNumber}")
    public ResponseEntity<Void> deleteAccount(@PathVariable String accountNumber) {
        accountService.deleteAccount(accountNumber);
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
    public ResponseEntity<Boolean> isAccountActive(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.isAccountActive(accountNumber));
    }

    /**
     * Checks if the specified account number belongs to the currently authenticated customer.
     *
     * @param accountNumber    the account number to verify ownership of
     * @param jwtUserPrincipal the authenticated customer's data
     * @return true if the account belongs to the current customer, false otherwise
     */
    @GetMapping("/me/owned")
    public ResponseEntity<Boolean> isAccountOwnedByCurrentCustomer(
            @RequestParam String accountNumber,
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal) {

        return ResponseEntity.ok(accountService.isAccountOwnedByCurrentCustomer(
                jwtUserPrincipal.customerId(), accountNumber));
    }

    // ============================================================
    // ==================== BALANCE OPERATIONS ====================
    // ============================================================

    /**
     * Retrieves the balance of a specific account.
     *
     * @param accountNumber the account number
     * @return the account balance
     */
    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<BigDecimal> getAccountBalanceByAccountNumber(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccountBalanceByAccountNumber(accountNumber));
    }

    /**
     * Retrieves the balances of all accounts of the authenticated user.
     *
     * @param jwtUserPrincipal the authenticated customer's data
     * @return list of account balances
     */
    @GetMapping("/me/balances")
    public ResponseEntity<List<AccountBalanceInfo>> getAllAccountsBalances(
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal) {

        return ResponseEntity.ok(accountService.getAllAccountsBalances(jwtUserPrincipal.customerId()));
    }

    /**
     * Retrieves the balance of the authenticated user's account by type.
     *
     * @param type             the account type
     * @param jwtUserPrincipal the authenticated customer's data
     * @return the account balance
     */
    @GetMapping("/me/balance")
    public ResponseEntity<BigDecimal> getMyAccountBalanceByType(
            @RequestParam AccountType type,
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal) {

        return ResponseEntity.ok(accountService.getAccountBalance(jwtUserPrincipal.customerId(), type));
    }

    /**
     * Checks if the authenticated user has sufficient funds in a specific account type.
     *
     * @param type             the account type
     * @param amount           the required amount
     * @param jwtUserPrincipal the authenticated customer's data
     * @return true if sufficient funds are available, false otherwise
     */
    @GetMapping("/me/has-funds")
    public ResponseEntity<Boolean> hasSufficientFundsByType(
            @RequestParam AccountType type,
            @RequestParam BigDecimal amount,
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal) {

        return ResponseEntity.ok(accountService.hasSufficientFundsByType(
                jwtUserPrincipal.customerId(), type, amount));
    }

    // ============================================================
    // ==================== INTERNAL OPERATIONS ===================
    // ============================================================

    /**
     * Endpoint to deposit funds into an account.
     *
     * @param depositBalanceRequest the deposit request containing account number and amount
     * @return 200 OK if deposit is successful
     */
    @PostMapping("/internal/deposit")
    public ResponseEntity<Void> performDeposit(
            @RequestBody ServiceDepositBalanceRequest depositBalanceRequest) {

        accountService.performDeposit(
                depositBalanceRequest.customerId(),
                new DepositBalanceRequest(
                        depositBalanceRequest.accountNumber(),
                        depositBalanceRequest.amount()));
        return ResponseEntity.ok().build();
    }

    /**
     * Endpoint to perform payments between accounts (used internally by services).
     *
     * @param servicePaymentRequest the payment request containing source, destination, and amount
     * @return 200 OK if payment is successful
     */
    @PostMapping("/internal/payment")
    public ResponseEntity<Void> performPayment(
            @RequestBody ServicePaymentRequest servicePaymentRequest) {

        accountService.performPayment(
                servicePaymentRequest.customerId(),
                new PaymentBalanceRequest(
                        servicePaymentRequest.fromAccountNumber(),
                        servicePaymentRequest.toAccountNumber(),
                        servicePaymentRequest.amount()
                )
        );
        return ResponseEntity.ok().build();
    }

    /**
     * Endpoint to perform refunds between accounts (used internally by services).
     *
     * @param serviceRefundBalanceRequest the refund request containing accounts and amount
     * @return 200 OK if refund is successful
     */
    @PostMapping("/internal/refund")
    public ResponseEntity<Void> performRefund(
            @RequestBody ServiceRefundBalanceRequest serviceRefundBalanceRequest) {

        accountService.performRefund(
                serviceRefundBalanceRequest.customerId(),
                new RefundBalanceRequest(
                        serviceRefundBalanceRequest.fromAccountNumber(),
                        serviceRefundBalanceRequest.toAccountNumber(),
                        serviceRefundBalanceRequest.amount()
                )
        );
        return ResponseEntity.ok().build();
    }
}
