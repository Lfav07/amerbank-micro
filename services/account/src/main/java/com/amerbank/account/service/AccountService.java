package com.amerbank.account.service;

import com.amerbank.account.dto.*;
import com.amerbank.account.exception.AccountNotFoundException;
import com.amerbank.account.exception.CustomerServiceUnavailableException;
import com.amerbank.account.model.Account;
import com.amerbank.account.model.AccountStatus;
import com.amerbank.account.repository.AccountRepository;
import com.amerbank.common_dto.CustomerResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;


@Service
@RequiredArgsConstructor
public class AccountService {

    private static final String PREFIX = "ACCT";
    private static final int BODY_DIGITS = 10;
    private static final long UPPER_BOUND = 1_000_000_0000L;
    private static final SecureRandom RNG = new SecureRandom();

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final RestTemplate restTemplate;

    // -------------------------------
    // ACCOUNT CREATION & GENERATION
    // -------------------------------

    /**
     * Generates a unique account number with the format "ACCTXXXXXXXXXX".
     *
     * @return a unique account number string
     */
    public String generateAccountNumber() {
        String candidate;
        do {
            long body = RNG.nextLong(UPPER_BOUND);
            candidate = PREFIX + String.format("%0" + BODY_DIGITS + "d", body);
        } while (accountRepository.existsByAccountNumber(candidate));
        return candidate;
    }

    /**
     * Creates a new account from the provided request.
     *
     * @param request the account creation request
     * @return the created AccountResponse DTO
     */
    public AccountResponse createAccount(AccountRequest request, String jwtToken) {
        Long customerId = getCustomerId(jwtToken);
        Account account = accountMapper.toAccount(request);
        account.setAccountNumber(generateAccountNumber());
        account.setCustomerId(customerId);
        account.setBalance(BigDecimal.ZERO);
        Account saved = accountRepository.save(account);
        return accountMapper.fromAccount(saved);
    }

    // -------------------------------
    // RETRIEVAL (GET) METHODS
    // -------------------------------

    /**
     * Get account by account number (cached).
     */
    @Cacheable(value = "accounts", key = "#accountNumber")
    public AccountResponse getAccountByAccountNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with accountNumber " + accountNumber));
        return accountMapper.fromAccount(account);
    }

    /**
     * Retrieves all accounts associated with a customer ID.
     *
     * @param customerId the UUID of the customer
     * @return a list of AccountResponse DTOs
     */
    public List<AccountResponse> getAccountsByCustomerId(Long customerId) {
        List<Account> accounts = accountRepository.findAllByCustomerId(customerId);
        return accounts.stream()
                .map(accountMapper::fromAccount)
                .toList();
    }

    /**
     * Retrieves the balance of the authenticated user's account.
     *
     * @param jwtToken the JWT token used for authentication
     * @return the BigDecimal balance of the account
     * @throws IllegalStateException if the balance is null
     */
    public BigDecimal getAccountBalance(String jwtToken) {
        AccountInfo accountInfo = getMyAccountInfo(jwtToken);
        BigDecimal balance = accountInfo.balance();
        if (balance == null) {
            throw new IllegalStateException("Account balance is null");
        }
        return balance;
    }

    /**
     * Retrieves the balance of the authenticated user's account.
     *
     * @param accountNumber the account number
     * @return the BigDecimal balance of the account
     * @throws IllegalStateException if the balance is null
     */
    public BigDecimal getAccountBalanceByAccountNumber(String accountNumber) {
        AccountResponse response = getAccountByAccountNumber(accountNumber);
        BigDecimal balance = response.balance();
        if (balance == null) {
            throw new IllegalStateException("Account balance is null");
        }
        return balance;
    }


    /**
     * @param accountNumber the account number
     * @return the current account status
     * @throws AccountNotFoundException if no account is found
     */
    public AccountStatus getAccountStatus(String accountNumber) {
        return findAccountEntity(accountNumber).getStatus();
    }

    // -------------------------------
    // UPDATE METHODS
    // -------------------------------

    /**
     * Updates the account type.
     *
     * @param request the account type update request
     * @return the updated AccountResponse
     * @throws AccountNotFoundException if the account does not exist
     */
    @CachePut(value = "accounts", key = "#request.accountNumber()")
    public AccountResponse updateAccountType(AccountUpdateTypeRequest request) {
        Account existing = findAccountEntity(request.accountNumber());
        existing.setType(request.type());
        return accountMapper.fromAccount(accountRepository.save(existing));
    }


    /**
     * Updates the account status.
     *
     * @param request the account status update request
     * @return the updated AccountResponse
     * @throws AccountNotFoundException if the account does not exist
     */
    @CachePut(value = "accounts", key = "#request.accountNumber()")
    public AccountResponse updateAccountStatus(AccountUpdateStatusRequest request) {
        Account existing = findAccountEntity(request.accountNumber());
        existing.setStatus(request.status());
        return accountMapper.fromAccount(accountRepository.save(existing));
    }

    /**
     * Suspends the account by setting its status to SUSPENDED.
     *
     * @param accountNumber the account number
     * @return the updated AccountResponse
     * @throws AccountNotFoundException if the account does not exist
     */
    @CachePut(value = "accounts", key = "#accountNumber")
    public AccountResponse suspendAccount(String accountNumber) {
        Account account = findAccountEntity(accountNumber);
        account.setStatus(AccountStatus.SUSPENDED);
        return accountMapper.fromAccount(accountRepository.save(account));
    }

    // -------------------------------
    // STATUS & UTILITY CHECKS
    // -------------------------------

    /**
     * Checks whether an account is ACTIVE.
     *
     * @param accountNumber the account number
     * @return true if the account is ACTIVE, false otherwise
     * @throws AccountNotFoundException if the account does not exist
     */
    public boolean isAccountActive(String accountNumber) {
        return findAccountEntity(accountNumber).getStatus().equals(AccountStatus.ACTIVE);
    }

    /**
     * Checks if the authenticated user has sufficient funds.
     *
     * @param jwtToken         the JWT token for the authenticated user
     * @param amountToTransfer the amount to validate
     * @return true if the balance is greater than or equal to the transfer amount
     * @throws IllegalStateException if the balance is null
     */
    public boolean hasSufficientFunds(String jwtToken, BigDecimal amountToTransfer) {
        BigDecimal balance = getAccountBalance(jwtToken);
        return balance.compareTo(amountToTransfer) >= 0;
    }

    // -------------------------------
    // DELETION
    // -------------------------------

    /**
     * Deletes an account by its account number.
     *
     * @param accountNumber the account number
     * @throws AccountNotFoundException if the account does not exist
     */
    @CacheEvict(value = "accounts", key = "#accountNumber")
    public void deleteAccount(String accountNumber) {
        Account account = findAccountEntity(accountNumber);
        accountRepository.delete(account);
    }

    // -------------------------------
    // INTERNAL HELPERS
    // -------------------------------

    /**
     * Finds and returns the Account entity for the given account number.
     *
     * @param accountNumber the account number
     * @return the corresponding Account entity
     * @throws AccountNotFoundException if no account is found
     */
    public Account findAccountEntity(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with accountNumber " + accountNumber));
    }

        /**
         * Retrieves the account information of the currently authenticated user
         * by contacting the customer service using the provided JWT token.
         *
         * @param jwtToken the JWT token of the authenticated user
         * @return Long customerId
         * @throws CustomerServiceUnavailableException if the customer service cannot be reached
         * @throws AccountNotFoundException            if the user or account cannot be found
         */
        public Long getCustomerId(String jwtToken) {
            String url = "http://customer/customer/me";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(jwtToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<CustomerResponse> response;
            try {
                response = restTemplate.exchange(url, HttpMethod.GET, entity, CustomerResponse.class);
            } catch (RestClientException e) {
                throw new CustomerServiceUnavailableException("Customer service unavailable");
            }

            CustomerResponse customerResponse = response.getBody();
            if (customerResponse == null || customerResponse.id() == null) {
                throw new AccountNotFoundException("Authenticated customer not found");
            }
            return customerResponse.id();
        }

    /**
     * Retrieves the account information of the currently authenticated user
     * by contacting the customer service using the provided JWT token.
     *
     * @param jwtToken the JWT token of the authenticated user
     * @return an AccountInfo DTO containing basic account data
     * @throws CustomerServiceUnavailableException if the customer service cannot be reached
     * @throws AccountNotFoundException            if the user or account cannot be found
     */
    public AccountInfo getMyAccountInfo(String jwtToken) {
        String url = "http://customer/customer/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<CustomerResponse> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.GET, entity, CustomerResponse.class);
        } catch (RestClientException e) {
            throw new CustomerServiceUnavailableException("Customer service unavailable");
        }

        CustomerResponse customerResponse = response.getBody();
        if (customerResponse == null || customerResponse.id() == null) {
            throw new AccountNotFoundException("Authenticated customer not found");
        }

        Account account = accountRepository.findByCustomerId(customerResponse.id())
                .orElseThrow(() -> new AccountNotFoundException("Account not found for authenticated customer"));

        AccountResponse accResponse = accountMapper.fromAccount(account);
        return accountMapper.getAccountInfo(accResponse);
    }
}

