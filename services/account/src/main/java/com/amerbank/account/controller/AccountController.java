package com.amerbank.account.controller;

import com.amerbank.account.dto.*;
import com.amerbank.account.exception.AccountNotFoundException;
import com.amerbank.account.model.Account;
import com.amerbank.account.model.AccountStatus;
import com.amerbank.account.model.AccountType;
import com.amerbank.account.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

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

    @GetMapping("/manage/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountByAccountNumber(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccountByAccountNumber(accountNumber));
    }

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

    @GetMapping("/manage/by-customer/{id}")
    public ResponseEntity<List<AccountResponse>> getAccountsByCustomerId(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getAccountsByCustomerId(id));
    }



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

    @GetMapping("/manage/{accountNumber}/balance")
    public ResponseEntity<BigDecimal> getAccountBalanceByAccountNumber(@PathVariable String accountNumber) {
        BigDecimal balance = accountService.getAccountBalanceByAccountNumber(accountNumber);
        return ResponseEntity.ok(balance);
    }


    @GetMapping("/manage/{accountNumber}/status")
    public ResponseEntity<AccountStatus> getAccountStatus(@PathVariable String accountNumber) {
        AccountStatus status = accountService.getAccountStatus(accountNumber);
        return ResponseEntity.ok(status);
    }


    @PutMapping("/manage/type")
    public ResponseEntity<AccountResponse> updateAccountType(@RequestBody AccountUpdateTypeRequest request) {
        AccountResponse updated = accountService.updateAccountType(request);
        return ResponseEntity.ok(updated);
    }


    @PutMapping("/manage/status")
    public ResponseEntity<AccountResponse> updateAccountStatus(@RequestBody AccountUpdateStatusRequest request) {
        AccountResponse updated = accountService.updateAccountStatus(request);
        return ResponseEntity.ok(updated);
    }


    @PutMapping("/manage/{accountNumber}/suspend")
    public ResponseEntity<AccountResponse> suspendAccount(@PathVariable String accountNumber) {
        AccountResponse suspended = accountService.suspendAccount(accountNumber);
        return ResponseEntity.ok(suspended);
    }


    @GetMapping("/manage/{accountNumber}/active")
    public ResponseEntity<Boolean> isAccountActive(@PathVariable String accountNumber) {
        boolean active = accountService.isAccountActive(accountNumber);
        return ResponseEntity.ok(active);
    }


    @GetMapping("/manage/me/has-funds")
    public ResponseEntity<Boolean> hasSufficientFunds(
            @RequestParam AccountType type,
            @RequestParam BigDecimal amount,
            HttpServletRequest request,
            Authentication authentication) {

        ResponseEntity<String> tokenResponse = extractJwtToken(request, authentication);
        if (!tokenResponse.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(tokenResponse.getStatusCode()).build();
        }
        String jwtToken = tokenResponse.getBody();
        boolean hasFunds = accountService.hasSufficientFunds(jwtToken, type, amount);
        return ResponseEntity.ok(hasFunds);
    }


    @DeleteMapping("/manage/{accountNumber}")
    public ResponseEntity<Void> deleteAccount(@PathVariable String accountNumber) {
        accountService.deleteAccount(accountNumber);
        return ResponseEntity.noContent().build();
    }
}
