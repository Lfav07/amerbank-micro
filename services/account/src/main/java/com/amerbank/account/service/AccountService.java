package com.amerbank.account.service;

import com.amerbank.account.config.AccountProperties;
import com.amerbank.account.dto.*;
import com.amerbank.account.exception.*;
import com.amerbank.account.model.Account;
import com.amerbank.account.model.AccountStatus;
import com.amerbank.account.model.AccountType;
import com.amerbank.account.repository.AccountRepository;
import com.amerbank.account.dto.DepositBalanceRequest;
import com.amerbank.account.dto.PaymentBalanceRequest;
import com.amerbank.account.dto.RefundBalanceRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service class responsible for managing accounts, including creation,
 * retrieval, updates, and deletion. It interacts with the database repository
 * and external customer service to validate and fetch customer information.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountProperties accountProperties;
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    // -------------------------------
    // ACCOUNT CREATION & GENERATION
    // -------------------------------

    /**
     * Generates a unique account number for a new account.
     * The account number consists of a configurable prefix and a numeric body
     * with a length defined by {@link AccountProperties}.
     * The numeric body is randomly generated using a thread-safe RNG.
     * <p>
     * Uniqueness is ensured by checking the repository for existing account numbers.
     * Retry attempts are made in case of collisions.
     *
     * @return a unique account number string
     * @throws IllegalStateException if a unique number cannot be generated after a few attempts
     */
    public String generateAccountNumber() {
        int maxAttempts = accountProperties.getMaxAttempts();

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            long body = ThreadLocalRandom.current().nextLong(accountProperties.getUpperBound());
            String candidate = accountProperties.getPrefix()
                    + String.format("%0" + accountProperties.getBodyDigits() + "d", body);

            if (!accountRepository.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate unique account number after " + maxAttempts + " attempts");
    }



    /**
     * Creates a new account for the authenticated customer with the given request details.
     * Throws an exception if the customer already has an account of the requested type.
     *
     * @param request    the account creation request details.
     * @param customerId the customer's id.
     * @return the created account response DTO.
     * @throws IllegalStateException if account of the requested type already exists for the customer.
     */
    @Transactional
    public AccountResponse createAccount(AccountRequest request, Long customerId) {

        Account account = accountMapper.toAccount(request);
        account.setAccountNumber(generateAccountNumber());
        account.setCustomerId(customerId);
        account.setBalance((accountProperties.getInitialBalance()));
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        log.info("Account {} created for customer {}",
                                account.getAccountNumber(), customerId);
                    }
                }
        );
        try {
            accountRepository.save(account);
        } catch (DataIntegrityViolationException e) {
            if (isUniqueConstraintViolation(e)) {
                log.warn("Duplicate account creation attempt for customerId {}", customerId);
                throw new AccountAlreadyExistsException(customerId, request.type());
            }
            log.error("Unexpected failure while creating account for customerId {}", customerId, e);
            throw new AccountRegistrationFailedException("Failed to create account", e);
        }
        return accountMapper.toResponse(account);
    }

    private boolean isUniqueConstraintViolation(DataIntegrityViolationException e) {
        Throwable t = e;
        while (t != null) {
            if (t instanceof ConstraintViolationException cve) {
                return "unique_customer_account_type".equals(cve.getConstraintName());
            }
            t = t.getCause();
        }
        return false;
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
    @Cacheable( value = "account-by-number", key = "#accountNumber")
    public AccountResponse getAccountByAccountNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with accountNumber " + accountNumber));
        return accountMapper.toResponse(account);
    }

    /**
     * Retrieves all accounts associated with a given customer ID.
     *
     * @param customerId the customer ID whose accounts are to be retrieved.
     * @return a list of account response DTOs.
     * @throws AccountNotFoundException if the customer has no accounts.
     */

    @Cacheable(
            value = "accounts-by-customer",
            key = "#customerId",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<AccountResponse> getAccountsByCustomerId(Long customerId) {
        List<Account> accounts = accountRepository.findAllByCustomerId(customerId);
        if (accounts.isEmpty()) {
            throw new AccountNotFoundException("No accounts found for customerId " + customerId);
        }
        return accounts.stream()
                .map(accountMapper::toResponse)
                .toList();
    }

    /**
     * Retrieves accounts of the authenticated customer.
     *
     * @param customerId the current customer's id.
     * @return a list of account info DTOs.
     * @throws AccountNotFoundException if the authenticated customer has no accounts.
     */


    public List<AccountInfo> getMyAccounts(Long customerId) {
        List<AccountResponse> accounts = getAccountsByCustomerId(customerId);
        if (accounts.isEmpty()) {
            throw new AccountNotFoundException("No accounts found for authenticated customer");
        }
        return accounts.stream()
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
        return accountMapper.toResponse(account);
    }

    /**
     * Retrieves the balance of the authenticated customer's account by account type.
     *
     * @param customerId the customer's id for the authenticated user.
     * @param type       the type of the account.
     * @return the account balance.
     * @throws AccountNotFoundException if no matching account is found.
     * @throws IllegalStateException    if the account balance is null.
     */
    public BigDecimal getAccountBalance(Long customerId, AccountType type) {
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
     * @param customerId the customer's id for the authenticated user.
     * @return a list of account balance info DTOs.
     * @throws AccountNotFoundException if the customer has no accounts.
     */
    public List<AccountBalanceInfo> getAllAccountsBalances(Long customerId) {
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
    public AccountResponse updateAccountType(String accountNumber, AccountUpdateTypeRequest request) {
        Account existing = findAccountEntity(accountNumber);
        Long customerId = existing.getCustomerId();

        if (accountRepository.existsByCustomerIdAndType(customerId, request.type())) {
            throw new IllegalStateException("Customer already has an account of type: " + request.type());
        }

        existing.setType(request.type());
        return accountMapper.toResponse(accountRepository.save(existing));
    }

    /**
     * Updates the status of an existing account.
     *
     * @param request the update request containing the account number and new status.
     * @return the updated account response DTO.
     * @throws AccountNotFoundException if the account is not found.
     */
    @CachePut(value = "accounts", key = "#request.accountNumber()")
    public AccountResponse updateAccountStatus(String accountNumber, AccountUpdateStatusRequest request) {
        Account existing = findAccountEntity(accountNumber);
        existing.setStatus(request.status());
        return accountMapper.toResponse(accountRepository.save(existing));
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
        return accountMapper.toResponse(accountRepository.save(account));
    }

    /**
     * Deposits a specified amount into the given account if the account belongs to the authenticated user.
     *
     * @param customerId the customer's id for the authenticated user.
     * @param request    contains account number and amount to deposit
     * @throws AccountNotFoundException if the account does not exist or does not belong to the user
     * @throws IllegalArgumentException if the deposit amount is negative or zero
     */
    @Transactional
    public void performDeposit(Long customerId, DepositBalanceRequest request) {
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        if (!isAccountActive(request.accountNumber())) {
            throw new InactiveAccountException("Account is not active");
        }

        Account account = findAccountEntityForUpdate(request.accountNumber());

        if (!account.getCustomerId().equals(customerId)) {
            throw new AccountNotFoundException("Account does not belong to authenticated customer");
        }

        account.setBalance(account.getBalance().add(request.amount()));
        accountRepository.save(account);
    }

    /**
     * Transfers a specified amount from one account to another, ensuring that the sender owns the source account
     * and has sufficient funds.
     *
     * @param customerId the customer's id for the authenticated user.
     * @param request    contains source (from) and destination (to) account numbers, and the payment amount
     * @throws AccountNotFoundException            if the source account does not exist or is not owned by the user
     * @throws IllegalArgumentException            if the payment amount is negative or zero
     * @throws InsufficientFundsAvailableException if the source account has insufficient balance
     */
    @Transactional
    public void performPayment(Long customerId, PaymentBalanceRequest request) {
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }

        if (request.fromAccountNumber().equals(request.toAccountNumber())) {
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }

        if (!isAccountActive(request.fromAccountNumber()) || !isAccountActive(request.toAccountNumber())) {
            throw new InactiveAccountException("Account is not active");
        }


        List<Account> locked = lockAccountsOrdered(
                request.fromAccountNumber(),
                request.toAccountNumber()
        );

        Account fromAccount = locked.stream()
                .filter(a -> a.getAccountNumber().equals(request.fromAccountNumber()))
                .findFirst()
                .orElseThrow();
        Account toAccount = locked.stream()
                .filter(a -> a.getAccountNumber().equals(request.toAccountNumber()))
                .findFirst()
                .orElseThrow();

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

    /**
     * Refunds a specified amount by reversing a previous transaction, transferring funds from the recipient
     * back to the original sender. Only the recipient of the original transaction can initiate a refund.
     *
     * @param customerId the customer's id for the authenticated user.
     * @param request    contains the account numbers involved in the original transaction and the refund amount
     * @throws AccountNotFoundException            if the sender (original recipient) account is not owned by the user
     * @throws IllegalArgumentException            if the refund amount is negative or zero
     * @throws InsufficientFundsAvailableException if the sender account lacks sufficient balance for the refund
     */
    @Transactional
    public void performRefund(Long customerId, RefundBalanceRequest request) {
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new IllegalArgumentException("Refund amount must be positive");
        }

        if (!isAccountActive(request.fromAccountNumber()) || !isAccountActive(request.toAccountNumber())) {
            throw new InactiveAccountException("Account is not active");
        }

        if (request.fromAccountNumber().equals(request.toAccountNumber())) {
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }


        List<Account> locked = lockAccountsOrdered(
                request.fromAccountNumber(),
                request.toAccountNumber()
        );

        BigDecimal amount = request.amount();


        // The one sending the refund is the RECIPIENT of the original transaction
        //  - original recipient
        Account sender = locked.stream()
                .filter(a -> a.getAccountNumber().equals(request.fromAccountNumber()))
                .findFirst()
                .orElseThrow();
        // original sender
        Account recipient =  locked.stream()
                .filter(a -> a.getAccountNumber().equals(request.toAccountNumber()))
                .findFirst()
                .orElseThrow();

        if (!sender.getCustomerId().equals(customerId)) {
            throw new AccountNotFoundException("Account does not belong to authenticated customer");
        }

        if (sender.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsAvailableException("Insufficient funds to perform refund.");
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        recipient.setBalance(recipient.getBalance().add(amount));

        accountRepository.save(sender);
        accountRepository.save(recipient);
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
     * @param customerId the customer's id for the authenticated user.
     * @param type             the account type.
     * @param amountToTransfer the amount to check against the balance.
     * @return true if the account balance is greater than or equal to the amount, false otherwise.
     * @throws AccountNotFoundException if no matching account is found.
     * @throws IllegalStateException    if the account balance is null.
     */
    public boolean hasSufficientFundsByType(Long customerId, AccountType type, BigDecimal amountToTransfer) {
        BigDecimal balance = getAccountBalance(customerId, type);
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
     * Meant for operations that involve an account's balance.
     * Uses pessimist locking.
     * @param accountNumber the account number to find.
     * @return the account entity.
     * @throws AccountNotFoundException if the account is not found.
     */
    public Account findAccountEntityForUpdate(String accountNumber) {
        return accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with accountNumber " + accountNumber));
    }

    /**
     * Locks an ordered account list using pessimist lock.
     * @param accountNumbers the account numbers to lock.
     * @return the accounts in their locked state.
     */
    private List<Account> lockAccountsOrdered(String... accountNumbers) {
        return Arrays.stream(accountNumbers)
                .distinct()
                .sorted()
                .map(this::lockAccount)
                .toList();
    }

    /**
     * Locks an account using pessimist lock.
     * @param accountNumber the account number to lock.
     * @return the account in its locked state.
     */
    private Account lockAccount(String accountNumber) {
        return accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found with accountNumber " + accountNumber));
    }

    /**
     * Finds an account entity by account number.
     *
     * @param accountNumber the account number to find.
     * @return true if the given accountNumber belongs to logged in customer
     * @throws AccountNotFoundException if the account is not found.
     */
    public boolean isAccountOwnedByCustomer(Long customerId, String accountNumber) {
        Account account = findAccountEntity(accountNumber);
        return account.getCustomerId().equals(customerId);
    }

}
