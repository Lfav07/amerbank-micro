package com.amerbank.account.service;

import com.amerbank.account.dto.*;
import com.amerbank.account.exception.AccountNotFoundException;
import com.amerbank.account.exception.CustomerServiceUnavailableException;
import com.amerbank.account.exception.InsufficientFundsAvailableException;
import com.amerbank.account.model.Account;
import com.amerbank.account.model.AccountStatus;
import com.amerbank.account.model.AccountType;
import com.amerbank.account.repository.AccountRepository;
import com.amerbank.common_dto.CustomerResponse;

import com.amerbank.common_dto.DepositBalanceRequest;
import com.amerbank.common_dto.PaymentBalanceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

/**
 * Service class responsible for managing accounts, including creation,
 * retrieval, updates, and deletion. It interacts with the database repository
 * and external customer service to validate and fetch customer information.
 */
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
     * Generates a unique account number with a fixed prefix and a 10-digit body.
     * Ensures that the generated account number does not already exist in the database.
     *
     * @return a unique account number string.
     */
    public String generateAccountNumber() {
        String candidate;
        do {
            long body = Math.abs(RNG.nextLong()) % UPPER_BOUND;
            candidate = PREFIX + String.format("%0" + BODY_DIGITS + "d", body);
        } while (accountRepository.existsByAccountNumber(candidate));
        return candidate;
    }

    /**
     * Creates a new account for the authenticated customer with the given request details.
     * Throws an exception if the customer already has an account of the requested type.
     *
     * @param request  the account creation request details.
     * @param jwtToken the JWT token of the authenticated user.
     * @return the created account response DTO.
     * @throws IllegalStateException if account of the requested type already exists for the customer.
     */
    public AccountResponse createAccount(AccountRequest request, String jwtToken) {
        Long customerId = getCustomerId(jwtToken);
        boolean exists = accountRepository.existsByCustomerIdAndType(customerId, request.type());
        if (exists) {
            throw new IllegalStateException("Customer already has an account of type: " + request.type());
        }
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
     * Retrieves an account by its account number.
     * Uses caching to optimize repeated lookups.
     *
     * @param accountNumber the account number to look up.
     * @return the account response DTO.
     * @throws AccountNotFoundException if no account with the given number exists.
     */
    @Cacheable(value = "accounts", key = "#accountNumber")
    public AccountResponse getAccountByAccountNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with accountNumber " + accountNumber));
        return accountMapper.fromAccount(account);
    }

    /**
     * Retrieves all accounts associated with a given customer ID.
     *
     * @param customerId the customer ID whose accounts are to be retrieved.
     * @return a list of account response DTOs.
     * @throws AccountNotFoundException if the customer has no accounts.
     */
    public List<AccountResponse> getAccountsByCustomerId(Long customerId) {
        List<Account> accounts = accountRepository.findAllByCustomerId(customerId);
        if (accounts.isEmpty()) {
            throw new AccountNotFoundException("No accounts found for customerId " + customerId);
        }
        return accounts.stream()
                .map(accountMapper::fromAccount)
                .toList();
    }

    /**
     * Retrieves accounts of the authenticated customer.
     *
     * @param jwtToken the JWT token of the authenticated user.
     * @return a list of account info DTOs.
     * @throws AccountNotFoundException if the authenticated customer has no accounts.
     */
    public List<AccountInfo> getMyAccounts(String jwtToken) {
        Long customerId = getCustomerId(jwtToken);
        List<Account> accounts = accountRepository.findAllByCustomerId(customerId);
        if (accounts.isEmpty()) {
            throw new AccountNotFoundException("No accounts found for authenticated customer");
        }
        return accounts.stream()
                .map(accountMapper::fromAccount)
                .map(accountMapper::getAccountInfo)
                .toList();
    }

    /**
     * Retrieves an account by customer ID and account type.
     *
     * @param customerId the customer ID.
     * @param type       the type of account.
     * @return the account response DTO.
     * @throws AccountNotFoundException if no matching account is found.
     */
    public AccountResponse getAccountByCustomerIdAndType(Long customerId, AccountType type) {
        Account account = accountRepository.findByCustomerIdAndType(customerId, type)
                .orElseThrow(() -> new AccountNotFoundException(
                        "No account found for customerId " + customerId + " and type " + type));
        return accountMapper.fromAccount(account);
    }

    /**
     * Retrieves the balance of the authenticated customer's account by account type.
     *
     * @param jwtToken the JWT token of the authenticated user.
     * @param type     the type of the account.
     * @return the account balance.
     * @throws AccountNotFoundException if no matching account is found.
     * @throws IllegalStateException    if the account balance is null.
     */
    public BigDecimal getAccountBalance(String jwtToken, AccountType type) {
        Long customerId = getCustomerId(jwtToken);
        Account account = accountRepository.findByCustomerIdAndType(customerId, type)
                .orElseThrow(() -> new AccountNotFoundException(
                        "No account found for authenticated customer with type " + type));
        BigDecimal balance = account.getBalance();
        if (balance == null) {
            throw new IllegalStateException("Account balance is null");
        }
        return balance;
    }

    /**
     * Retrieves balances of all accounts belonging to the authenticated customer.
     *
     * @param jwtToken the JWT token of the authenticated user.
     * @return a list of account balance info DTOs.
     * @throws AccountNotFoundException if the customer has no accounts.
     */
    public List<AccountBalanceInfo> getAllAccountsBalances(String jwtToken) {
        Long customerId = getCustomerId(jwtToken);
        List<Account> accounts = accountRepository.findAllByCustomerId(customerId);
        if (accounts.isEmpty()) {
            throw new AccountNotFoundException("No accounts found for authenticated customer");
        }
        return accounts.stream()
                .map(account -> new AccountBalanceInfo(
                        account.getAccountNumber(),
                        account.getType(),
                        account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO))
                .toList();
    }

    /**
     * Retrieves the balance of an account by account number.
     *
     * @param accountNumber the account number.
     * @return the account balance.
     * @throws AccountNotFoundException if no such account exists.
     * @throws IllegalStateException    if the account balance is null.
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
     * Retrieves the status of an account by its account number.
     *
     * @param accountNumber the account number.
     * @return the account status.
     * @throws AccountNotFoundException if the account is not found.
     */
    public AccountStatus getAccountStatus(String accountNumber) {
        return findAccountEntity(accountNumber).getStatus();
    }

    // -------------------------------
    // UPDATE METHODS
    // -------------------------------

    /**
     * Updates the type of existing account.
     *
     * @param request the update request containing the account number and new type.
     * @return the updated account response DTO.
     * @throws IllegalStateException    if customer already has an account of the new type.
     * @throws AccountNotFoundException if the account is not found.
     */
    @CachePut(value = "accounts", key = "#request.accountNumber()")
    public AccountResponse updateAccountType(AccountUpdateTypeRequest request) {
        Account existing = findAccountEntity(request.accountNumber());
        Long customerId = existing.getCustomerId();

        if (accountRepository.existsByCustomerIdAndType(customerId, request.type())) {
            throw new IllegalStateException("Customer already has an account of type: " + request.type());
        }

        existing.setType(request.type());
        return accountMapper.fromAccount(accountRepository.save(existing));
    }

    /**
     * Updates the status of an existing account.
     *
     * @param request the update request containing the account number and new status.
     * @return the updated account response DTO.
     * @throws AccountNotFoundException if the account is not found.
     */
    @CachePut(value = "accounts", key = "#request.accountNumber()")
    public AccountResponse updateAccountStatus(AccountUpdateStatusRequest request) {
        Account existing = findAccountEntity(request.accountNumber());
        existing.setStatus(request.status());
        return accountMapper.fromAccount(accountRepository.save(existing));
    }

    /**
     * Suspends an account by setting its status to SUSPENDED.
     *
     * @param accountNumber the account number to suspend.
     * @return the updated account response DTO.
     * @throws AccountNotFoundException if the account is not found.
     */
    @CachePut(value = "accounts", key = "#accountNumber")
    public AccountResponse suspendAccount(String accountNumber) {
        Account account = findAccountEntity(accountNumber);
        account.setStatus(AccountStatus.SUSPENDED);
        return accountMapper.fromAccount(accountRepository.save(account));
    }

    /**
     * Deposits a specified amount into the given account if the account belongs to the authenticated user.
     *
     * @param jwtToken JWT token of the current user
     * @param request  contains account number and amount to deposit
     * @throws AccountNotFoundException if the account does not exist or does not belong to the user
     * @throws IllegalArgumentException if the deposit amount is negative or zero
     */
    @Transactional
    public void performDeposit(String jwtToken, DepositBalanceRequest request) {
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }

        Account account = findAccountEntity(request.accountNumber());

        Long customerId = getCustomerId(jwtToken);
        if (!account.getCustomerId().equals(customerId)) {
            throw new AccountNotFoundException("Account does not belong to authenticated customer");
        }

        account.setBalance(account.getBalance().add(request.amount()));
        accountRepository.save(account);
    }

    @Transactional
    public void performPayment(String jwtToken, PaymentBalanceRequest request) {
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        Long customerId = getCustomerId(jwtToken);
        Account fromAccount = findAccountEntity(request.fromAccountNumber());
        Account toAccount = findAccountEntity(request.toAccountNumber());

        if (!fromAccount.getCustomerId().equals(customerId)) {
            throw new AccountNotFoundException("Account does not belong to authenticated customer");
        }

        BigDecimal amount = request.amount();

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsAvailableException("Insufficient funds");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
    }


    // -------------------------------
    // STATUS & UTILITY CHECKS
    // -------------------------------

    /**
     * Checks whether an account is currently active.
     *
     * @param accountNumber the account number.
     * @return true if the account status is ACTIVE, false otherwise.
     * @throws AccountNotFoundException if the account is not found.
     */
    public boolean isAccountActive(String accountNumber) {
        return findAccountEntity(accountNumber).getStatus().equals(AccountStatus.ACTIVE);
    }

    /**
     * Checks if the authenticated customer has sufficient funds in an account of the given type.
     *
     * @param jwtToken         the JWT token of the authenticated user.
     * @param type             the account type.
     * @param amountToTransfer the amount to check against the balance.
     * @return true if the account balance is greater than or equal to the amount, false otherwise.
     * @throws AccountNotFoundException if no matching account is found.
     * @throws IllegalStateException    if the account balance is null.
     */
    public boolean hasSufficientFundsByType(String jwtToken, AccountType type, BigDecimal amountToTransfer) {
        BigDecimal balance = getAccountBalance(jwtToken, type);
        return balance.compareTo(amountToTransfer) >= 0;
    }

    /**
     * Checks if the authenticated customer has sufficient funds in an account of the given type.
     *
     * @param accountNumber    the account number.
     * @param amountToTransfer the amount to check against the balance.
     * @return true if the account balance is greater than or equal to the amount, false otherwise.
     * @throws AccountNotFoundException if no matching account is found.
     * @throws IllegalStateException    if the account balance is null.
     */
    public boolean hasSufficientFundsByAccountNumber(String accountNumber, BigDecimal amountToTransfer) {
        BigDecimal balance = getAccountBalanceByAccountNumber(accountNumber);
        return balance.compareTo(amountToTransfer) >= 0;
    }

    // -------------------------------
    // DELETION
    // -------------------------------

    /**
     * Deletes an account by its account number.
     *
     * @param accountNumber the account number to delete.
     * @throws AccountNotFoundException if the account is not found.
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
     * Finds an account entity by account number.
     *
     * @param accountNumber the account number to find.
     * @return the account entity.
     * @throws AccountNotFoundException if the account is not found.
     */
    public Account findAccountEntity(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with accountNumber " + accountNumber));
    }

    /**
     * Finds an account entity by account number.
     *
     * @param accountNumber the account number to find.
     * @return true if the given accountNumber belongs to logged in customer
     * @throws AccountNotFoundException if the account is not found.
     */
    public boolean isAccountOwnedByCurrentCustomer(String jwtToken, String accountNumber) {
        Long customerId = getCustomerId(jwtToken);

        Account account = findAccountEntity(accountNumber);

        return account.getCustomerId().equals(customerId);
    }


    /**
     * Retrieves the customer ID of the authenticated user by calling the customer service.
     *
     * @param jwtToken the JWT token of the authenticated user.
     * @return the customer ID.
     * @throws CustomerServiceUnavailableException if the customer service is unavailable.
     * @throws AccountNotFoundException            if the authenticated customer is not found.
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
}
