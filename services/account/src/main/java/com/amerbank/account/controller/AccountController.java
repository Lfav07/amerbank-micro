package com.amerbank.account.controller;

import com.amerbank.account.dto.*;
import com.amerbank.account.model.AccountStatus;
import com.amerbank.account.model.AccountType;
import com.amerbank.account.service.AccountService;
import com.amerbank.common_dto.DepositBalanceRequest;
import com.amerbank.common_dto.PaymentBalanceRequest;
import com.amerbank.common_dto.RefundBalanceRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST controller for managing account-related operations.
 */
@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * Extracts the JWT token from the Authorization header.
     *
     * @param request the HTTP servlet request
     * @param authentication the authentication context
     * @return the JWT token if available and valid, otherwise an error response
     */
    private ResponseEntity<String> extractJwtToken(HttpServletRequest request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(authHeader.substring(7));
    }

    /**
     * Registers a new account for the authenticated user.
     *
     * @param accountRequest the account creation data
     * @param authentication the authentication context
     * @param request the HTTP servlet request
     * @return a success message or an error response
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerAccount(@RequestBody AccountRequest accountRequest, Authentication authentication, HttpServletRequest request) {
        ResponseEntity<String> tokenResponse = extractJwtToken(request, authentication);
        if (!tokenResponse.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(tokenResponse.getStatusCode()).build();
        }
        String jwtToken = tokenResponse.getBody();
        accountService.createAccount(accountRequest, jwtToken);
        return ResponseEntity.ok("Account successfully registered");
    }
    /**
     * Checks if the specified account number belongs to the currently authenticated customer.
     *
     * @param accountNumber the account number to verify ownership of
     * @param request the HttpServletRequest, used to extract the Authorization header
     * @param authentication the Spring Security Authentication object representing the current user
     * @return a ResponseEntity containing true if the account belongs to the current customer,
     *         false otherwise; returns 401 Unauthorized if JWT token is missing or invalid
     */
    @GetMapping("/manage/owned")
    public ResponseEntity<Boolean> isAccountOwnedByCurrentCustomer(
            @RequestParam String accountNumber,
            HttpServletRequest request,
            Authentication authentication) {

        ResponseEntity<String> tokenResponse = extractJwtToken(request, authentication);
        if (!tokenResponse.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(tokenResponse.getStatusCode()).build();
        }
        String jwtToken = tokenResponse.getBody();

        boolean owned = accountService.isAccountOwnedByCurrentCustomer(jwtToken, accountNumber);
        return ResponseEntity.ok(owned);
    }


    /**
     * Retrieves account details by account number.
     *
     * @param accountNumber the account number
     * @return account response with account information
     */
    @GetMapping("/manage/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountByAccountNumber(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccountByAccountNumber(accountNumber));
    }



    /**
     * Retrieves account(s) by customer ID and optionally filters by type.
     *
     * @param customerId the customer ID
     * @param type the account type (optional)
     * @return list of matching accounts
     */
    @GetMapping("/manage/by-customer/{customerId}/filter")
    public ResponseEntity<List<AccountResponse>> getAccountsByCustomerIdAndType(
            @PathVariable Long customerId,
            @RequestParam(required = false) AccountType type) {

        List<AccountResponse> accounts;
        if (type != null) {
            accounts = List.of(accountService.getAccountByCustomerIdAndType(customerId, type));
        } else {
            accounts = accountService.getAccountsByCustomerId(customerId);
        }
        return ResponseEntity.ok(accounts);
    }

    /**
     * Retrieves all accounts for a customer.
     *
     * @param id the customer ID
     * @return list of all accounts
     */
    @GetMapping("/manage/by-customer/{id}")
    public ResponseEntity<List<AccountResponse>> getAccountsByCustomerId(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getAccountsByCustomerId(id));
    }

    /**
     * Retrieves the authenticated user's accounts.
     *
     * @param request the HTTP servlet request
     * @param authentication the authentication context
     * @return list of account info
     */
    @GetMapping("/manage/me")
    public ResponseEntity<List<AccountInfo>> getMyAccounts(HttpServletRequest request, Authentication authentication) {
        ResponseEntity<String> tokenResponse = extractJwtToken(request, authentication);
        if (!tokenResponse.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(tokenResponse.getStatusCode()).build();
        }
        String jwtToken = tokenResponse.getBody();
        List<AccountInfo> accounts = accountService.getMyAccounts(jwtToken);
        return ResponseEntity.ok(accounts);
    }

    /**
     * Retrieves the balance of the authenticated user's account by type.
     *
     * @param type the account type
     * @param request the HTTP servlet request
     * @param authentication the authentication context
     * @return the account balance
     */
    @GetMapping("/manage/me/balance")
    public ResponseEntity<BigDecimal> getMyAccountBalanceByType(
            @RequestParam AccountType type,
            HttpServletRequest request,
            Authentication authentication) {

        ResponseEntity<String> tokenResponse = extractJwtToken(request, authentication);
        if (!tokenResponse.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(tokenResponse.getStatusCode()).build();
        }
        String jwtToken = tokenResponse.getBody();

        BigDecimal balance = accountService.getAccountBalance(jwtToken, type);
        return ResponseEntity.ok(balance);
    }

    /**
     * Retrieves the balances of all accounts of the authenticated user.
     *
     * @param request the HTTP servlet request
     * @param authentication the authentication context
     * @return list of account balances
     */
    @GetMapping("/manage/me/balances")
    public ResponseEntity<List<AccountBalanceInfo>> getAllAccountsBalances(HttpServletRequest request, Authentication authentication) {
        ResponseEntity<String> tokenResponse = extractJwtToken(request, authentication);
        if (!tokenResponse.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(tokenResponse.getStatusCode()).build();
        }
        String jwtToken = tokenResponse.getBody();

        List<AccountBalanceInfo> balances = accountService.getAllAccountsBalances(jwtToken);
        return ResponseEntity.ok(balances);
    }

    /**
     * Retrieves the balance of a specific account.
     *
     * @param accountNumber the account number
     * @return the account balance
     */
    @GetMapping("/manage/{accountNumber}/balance")
    public ResponseEntity<BigDecimal> getAccountBalanceByAccountNumber(@PathVariable String accountNumber) {
        BigDecimal balance = accountService.getAccountBalanceByAccountNumber(accountNumber);
        return ResponseEntity.ok(balance);
    }

    /**
     * Retrieves the status of a specific account.
     *
     * @param accountNumber the account number
     * @return the account status
     */
    @GetMapping("/manage/{accountNumber}/status")
    public ResponseEntity<AccountStatus> getAccountStatus(@PathVariable String accountNumber) {
        AccountStatus status = accountService.getAccountStatus(accountNumber);
        return ResponseEntity.ok(status);
    }

    /**
     * Updates the type of account.
     *
     * @param request the account update request
     * @return the updated account information
     */
    @PutMapping("/manage/type")
    public ResponseEntity<AccountResponse> updateAccountType(@RequestBody AccountUpdateTypeRequest request) {
        AccountResponse updated = accountService.updateAccountType(request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Endpoint to deposit funds into an account.
     * Expects a valid JWT and a payload with account number and amount.
     *
     * @param request the HttpServletRequest, used to extract the Authorization header
     * @param authentication the Spring Security Authentication object representing the current user
     *
     * @param depositBalanceRequest the deposit request containing account number and amount
     * @return 200 OK if deposit is successful
     */
    @PostMapping("/deposit")
    public ResponseEntity<Void> performDeposit(
            @RequestBody DepositBalanceRequest depositBalanceRequest,
            HttpServletRequest request,
            Authentication authentication){
        ResponseEntity<String> tokenResponse = extractJwtToken(request, authentication);
        if (!tokenResponse.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(tokenResponse.getStatusCode()).build();
        }

        String jwtToken = tokenResponse.getBody();

        accountService.performDeposit(jwtToken, depositBalanceRequest);
        return ResponseEntity.ok().build();
    }

    /**
     * Endpoint to transfer funds between accounts.
     * Expects a valid JWT and a payload with accounts numbers and amount.
     *
     * @param request the HttpServletRequest, used to extract the Authorization header
     * @param authentication the Spring Security Authentication object representing the current user
     *
     * @param paymentBalanceRequest the payment request containing accounts numbers and amount
     * @return 200 OK if payment is successful
     */
    @PostMapping("/payment")
    public ResponseEntity<Void> performPayment(
            @RequestBody PaymentBalanceRequest paymentBalanceRequest,
            HttpServletRequest request,
            Authentication authentication){
        ResponseEntity<String> tokenResponse = extractJwtToken(request, authentication);
        if (!tokenResponse.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(tokenResponse.getStatusCode()).build();
        }

        String jwtToken = tokenResponse.getBody();

        accountService.performPayment(jwtToken, paymentBalanceRequest);
        return ResponseEntity.ok().build();
    }
    /**
     * Endpoint to perform refunds between accounts.
     * Expects a valid JWT and a payload with accounts numbers and amount.
     *
     * @param request the HttpServletRequest, used to extract the Authorization header
     * @param authentication the Spring Security Authentication object representing the current user
     *
     * @param refundBalanceRequest the refund request containing accounts numbers and amount
     * @return 200 OK if payment is successful
     */
    @PostMapping("/refund")
    public ResponseEntity<Void> performRefund(
            @RequestBody RefundBalanceRequest refundBalanceRequest,
            HttpServletRequest request,
            Authentication authentication){
        ResponseEntity<String> tokenResponse = extractJwtToken(request, authentication);
        if (!tokenResponse.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(tokenResponse.getStatusCode()).build();
        }

        String jwtToken = tokenResponse.getBody();

        accountService.performRefund(jwtToken, refundBalanceRequest);
        return ResponseEntity.ok().build();
    }


    /**
     * Updates the status of an account.
     *
     * @param request the account update request
     * @return the updated account information
     */
    @PutMapping("/manage/status")
    public ResponseEntity<AccountResponse> updateAccountStatus(@RequestBody AccountUpdateStatusRequest request) {
        AccountResponse updated = accountService.updateAccountStatus(request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Suspends a specific account.
     *
     * @param accountNumber the account number
     * @return the updated account information after suspension
     */
    @PutMapping("/manage/{accountNumber}/suspend")
    public ResponseEntity<AccountResponse> suspendAccount(@PathVariable String accountNumber) {
        AccountResponse suspended = accountService.suspendAccount(accountNumber);
        return ResponseEntity.ok(suspended);
    }

    /**
     * Checks if an account is currently active.
     *
     * @param accountNumber the account number
     * @return true if the account is active, false otherwise
     */
    @GetMapping("/manage/{accountNumber}/active")
    public ResponseEntity<Boolean> isAccountActive(@PathVariable String accountNumber) {
        boolean active = accountService.isAccountActive(accountNumber);
        return ResponseEntity.ok(active);
    }

    /**
     * Checks if the authenticated user has sufficient funds in a specific account type.
     *
     * @param type the account type
     * @param amount the required amount
     * @param request the HTTP servlet request
     * @param authentication the authentication context
     * @return true if sufficient funds are available, false otherwise
     */
    @GetMapping("/manage/me/has-funds")
    public ResponseEntity<Boolean> hasSufficientFundsByType(
            @RequestParam AccountType type,
            @RequestParam BigDecimal amount,
            HttpServletRequest request,
            Authentication authentication) {

        ResponseEntity<String> tokenResponse = extractJwtToken(request, authentication);
        if (!tokenResponse.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(tokenResponse.getStatusCode()).build();
        }
        String jwtToken = tokenResponse.getBody();
        boolean hasFunds = accountService.hasSufficientFundsByType(jwtToken, type, amount);
        return ResponseEntity.ok(hasFunds);
    }



    /**
     * Deletes an account by its account number.
     *
     * @param accountNumber the account number
     * @return a 204 No Content response if deletion is successful
     */
    @DeleteMapping("/manage/{accountNumber}")
    public ResponseEntity<Void> deleteAccount(@PathVariable String accountNumber) {
        accountService.deleteAccount(accountNumber);
        return ResponseEntity.noContent().build();
    }
}
