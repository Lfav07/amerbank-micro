package com.amerbank.account.controller;

import com.amerbank.account.dto.*;
import com.amerbank.account.model.AccountType;
import com.amerbank.account.security.JwtUserPrincipal;
import com.amerbank.account.service.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST controller for customer-facing account operations.
 * Handles account registration, retrieval, and balance inquiries for authenticated users.
 */
@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
@Validated
public class AccountController {

    private final AccountService accountService;

    // ============================================================
    // =============== ACCOUNT REGISTRATION & RETRIEVAL ===========
    // ============================================================

    /**
     * Registers a new account for the authenticated user.
     *
     * @param accountRequest   the account creation data
     * @param jwtUserPrincipal the authenticated customer's data
     * @return a success message
     */
    @PostMapping("/register")
    public ResponseEntity<String> registerAccount(
            @Valid @RequestBody AccountRequest accountRequest,
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal) {

        accountService.createAccount(accountRequest, jwtUserPrincipal.customerId());
        return ResponseEntity.ok("Account successfully registered");
    }

    /**
     * Retrieves all accounts for the authenticated user.
     *
     * @param jwtUserPrincipal the authenticated customer's data
     * @return list of account information
     */
    @GetMapping("/me")
    public ResponseEntity<List<AccountInfo>> getMyAccounts(
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal) {

        List<AccountInfo> accounts = accountService.getMyAccounts(jwtUserPrincipal.customerId());
        return ResponseEntity.ok(accounts);
    }

    /**
     * Retrieves a specific account by type for the authenticated user.
     *
     * @param type             the account type to retrieve
     * @param jwtUserPrincipal the authenticated customer's data
     * @return the account information
     */
    @GetMapping("/me/type")
    public ResponseEntity<AccountInfo> getMyAccountByType(
            @RequestParam @NotNull AccountType type,
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal) {

        AccountInfo account = accountService.getMyAccountByType(jwtUserPrincipal.customerId(), type);
        return ResponseEntity.ok(account);
    }

    /**
     * Checks if the specified account number belongs to the authenticated customer.
     *
     * @param request          DTO containing the account number to verify
     * @param jwtUserPrincipal the authenticated customer's data
     * @return true if the account belongs to the authenticated customer, false otherwise
     */
    @PostMapping("/me/owned")
    public ResponseEntity<Boolean> isAccountOwnedByCurrentCustomer(
            @Valid @RequestBody MyAccountOwnedRequest request,
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal) {

        boolean isOwned = accountService.isAccountOwnedByCustomer(
                jwtUserPrincipal.customerId(),
                request.accountNumber()
        );
        return ResponseEntity.ok(isOwned);
    }

    // ============================================================
    // ==================== BALANCE OPERATIONS ====================
    // ============================================================

    /**
     * Retrieves the balances of all accounts for the authenticated user.
     *
     * @param jwtUserPrincipal the authenticated customer's data
     * @return list of account balances
     */
    @GetMapping("/me/balances")
    public ResponseEntity<List<AccountBalanceInfo>> getAllAccountsBalances(
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal) {

        List<AccountBalanceInfo> balances = accountService.getAllAccountsBalances(
                jwtUserPrincipal.customerId()
        );
        return ResponseEntity.ok(balances);
    }

    /**
     * Retrieves the balance of a specific account type for the authenticated user.
     *
     * @param type             the account type
     * @param jwtUserPrincipal the authenticated customer's data
     * @return the account balance
     */
    @GetMapping("/me/balance")
    public ResponseEntity<BigDecimal> getMyAccountBalanceByType(
            @RequestParam @NotNull AccountType type,
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal) {

        BigDecimal balance = accountService.getAccountBalance(
                jwtUserPrincipal.customerId(),
                type
        );
        return ResponseEntity.ok(balance);
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
            @RequestParam @NotNull AccountType type,
            @RequestParam @NotNull @DecimalMin(value = "0.01", message = "Amount must be greater than zero") BigDecimal amount,
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal) {

        boolean hasFunds = accountService.hasSufficientFundsByType(
                jwtUserPrincipal.customerId(),
                type,
                amount
        );
        return ResponseEntity.ok(hasFunds);
    }
}