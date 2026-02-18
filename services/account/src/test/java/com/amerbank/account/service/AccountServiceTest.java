package com.amerbank.account.service;

import com.amerbank.account.config.AccountProperties;
import com.amerbank.account.dto.*;
import com.amerbank.account.exception.*;
import com.amerbank.account.model.Account;
import com.amerbank.account.model.AccountStatus;
import com.amerbank.account.model.AccountType;
import com.amerbank.account.repository.AccountRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Spy
    private AccountMapper accountMapper;

    private AccountProperties accountProperties;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountProperties = new AccountProperties();
        accountProperties.setPrefix("ACC");
        accountProperties.setBodyDigits(10);
        accountProperties.setUpperBound(10000000000L);
        accountProperties.setMaxAttempts(5);
        accountProperties.setInitialBalance(BigDecimal.valueOf(100.00));
        TransactionSynchronizationManager.initSynchronization();

        accountService = new AccountService(
                accountProperties,
                accountRepository,
                accountMapper
        );
    }

    @AfterEach
    public void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    // ==================== Account Generation Tests ====================

    @Test
    @DisplayName("Should generate unique account number")
    void shouldGenerateUniqueAccountNumber() {
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);

        String accountNumber = accountService.generateAccountNumber();

        assertNotNull(accountNumber);
        assertTrue(accountNumber.startsWith("ACC"));
        assertEquals(13, accountNumber.length());
    }

    @Test
    @DisplayName("Should throw FailedToGenerateAccountNumberException after max attempts")
    void shouldThrowExceptionAfterMaxAttempts() {
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(true);

        assertThrows(FailedToGenerateAccountNumberException.class, () -> {
            accountService.generateAccountNumber();
        });
    }

    // ==================== Account Creation Tests ====================

    @Test
    @DisplayName("Should create account successfully")
    void shouldCreateAccountSuccessfully() {
        Long customerId = 1L;
        AccountRequest request = new AccountRequest(AccountType.CHECKING);

        Account savedAccount = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("ACC0000000001")
                .customerId(customerId)
                .balance(BigDecimal.valueOf(100.00))
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

        AccountResponse response = accountService.createAccount(request, customerId);

        assertNotNull(response);
        assertEquals(customerId, response.customerId());
        assertEquals(AccountType.CHECKING, response.type());
        assertEquals(AccountStatus.ACTIVE, response.status());

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());

        Account capturedAccount = accountCaptor.getValue();
        assertEquals(customerId, capturedAccount.getCustomerId());
        assertEquals(AccountType.CHECKING, capturedAccount.getType());
        assertEquals(AccountStatus.ACTIVE, capturedAccount.getStatus());
        assertNotNull(capturedAccount.getAccountNumber());
    }

    @Test
    @DisplayName("Should throw AccountAlreadyExistsException on data integrity violation")
    void shouldThrowAccountAlreadyExistsOnDataIntegrityViolation() {
        Long customerId = 1L;
        AccountRequest request = new AccountRequest(AccountType.CHECKING);

        org.hibernate.exception.ConstraintViolationException cve =
                mock(org.hibernate.exception.ConstraintViolationException.class);
        when(cve.getConstraintName()).thenReturn("unique_customer_account_type");

        DataIntegrityViolationException dive = new DataIntegrityViolationException("Constraint violation", cve);

        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenThrow(dive);

        assertThrows(AccountAlreadyExistsException.class, () -> {
            accountService.createAccount(request, customerId);
        });
    }

    // ==================== Retrieval Tests ====================

    @Test
    @DisplayName("Should find account by account number")
    void shouldFindAccountByAccountNumber() {
        String accountNumber = "ACC0000000001";
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber(accountNumber)
                .customerId(1L)
                .balance(BigDecimal.valueOf(100.00))
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        AccountResponse response = accountService.getAccountByAccountNumber(accountNumber);

        assertNotNull(response);
        assertEquals(accountNumber, response.accountNumber());
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException when account number not found")
    void shouldThrowAccountNotFoundExceptionWhenAccountNumberNotFound() {
        String accountNumber = "ACC9999999999";
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> {
            accountService.getAccountByAccountNumber(accountNumber);
        });
    }

    @Test
    @DisplayName("Should find accounts by customer ID")
    void shouldFindAccountsByCustomerId() {
        Long customerId = 1L;
        Account account1 = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("ACC0000000001")
                .customerId(customerId)
                .balance(BigDecimal.valueOf(100.00))
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();
        Account account2 = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("ACC0000000002")
                .customerId(customerId)
                .balance(BigDecimal.valueOf(200.00))
                .type(AccountType.SAVINGS)
                .status(AccountStatus.ACTIVE)
                .build();

        List<Account> accounts = Arrays.asList(account1, account2);
        when(accountRepository.findAllByCustomerId(customerId)).thenReturn(accounts);

        List<AccountResponse> responses = accountService.getAccountsByCustomerId(customerId);

        assertNotNull(responses);
        assertEquals(2, responses.size());
    }

    @Test
    @DisplayName("Should get account by customer ID and type")
    void shouldGetAccountByCustomerIdAndType() {
        Long customerId = 1L;
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("ACC0000000001")
                .customerId(customerId)
                .balance(BigDecimal.valueOf(100.00))
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findByCustomerIdAndType(customerId, AccountType.CHECKING))
                .thenReturn(Optional.of(account));

        AccountResponse response = accountService.getAccountByCustomerIdAndType(customerId, AccountType.CHECKING);

        assertNotNull(response);
        assertEquals(AccountType.CHECKING, response.type());
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException when customer and type combination not found")
    void shouldThrowAccountNotFoundExceptionWhenCustomerAndTypeNotFound() {
        Long customerId = 1L;
        when(accountRepository.findByCustomerIdAndType(customerId, AccountType.CHECKING))
                .thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> {
            accountService.getAccountByCustomerIdAndType(customerId, AccountType.CHECKING);
        });
    }

    @Test
    @DisplayName("Should get account balance by customer ID and type")
    void shouldGetAccountBalanceByCustomerIdAndType() {
        Long customerId = 1L;
        BigDecimal expectedBalance = BigDecimal.valueOf(500.00);
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("ACC0000000001")
                .customerId(customerId)
                .balance(expectedBalance)
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findByCustomerIdAndType(customerId, AccountType.CHECKING))
                .thenReturn(Optional.of(account));

        BigDecimal balance = accountService.getAccountBalance(customerId, AccountType.CHECKING);

        assertEquals(expectedBalance, balance);
    }

    @Test
    @DisplayName("Should throw NullAccountBalanceException when balance is null")
    void shouldThrowNullAccountBalanceExceptionWhenBalanceIsNull() {
        Long customerId = 1L;
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("ACC0000000001")
                .customerId(customerId)
                .balance(null)
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findByCustomerIdAndType(customerId, AccountType.CHECKING))
                .thenReturn(Optional.of(account));

        assertThrows(NullAccountBalanceException.class, () -> {
            accountService.getAccountBalance(customerId, AccountType.CHECKING);
        });
    }

    @Test
    @DisplayName("Should get all account balances for customer")
    void shouldGetAllAccountBalances() {
        Long customerId = 1L;
        Account account1 = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("ACC0000000001")
                .customerId(customerId)
                .balance(BigDecimal.valueOf(100.00))
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();
        Account account2 = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("ACC0000000002")
                .customerId(customerId)
                .balance(BigDecimal.valueOf(200.00))
                .type(AccountType.SAVINGS)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findAllByCustomerId(customerId)).thenReturn(Arrays.asList(account1, account2));

        List<AccountBalanceInfo> balances = accountService.getAllAccountsBalances(customerId);

        assertNotNull(balances);
        assertEquals(2, balances.size());
    }

    @Test
    @DisplayName("Should get my accounts")
    void shouldGetMyAccounts() {
        Long customerId = 1L;
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("ACC0000000001")
                .customerId(customerId)
                .balance(BigDecimal.valueOf(100.00))
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findAllByCustomerId(customerId)).thenReturn(List.of(account));

        List<AccountInfo> accounts = accountService.getMyAccounts(customerId);

        assertNotNull(accounts);
        assertEquals(1, accounts.size());
    }

    @Test
    @DisplayName("Should get my account by type")
    void shouldGetMyAccountByType() {
        Long customerId = 1L;
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("ACC0000000001")
                .customerId(customerId)
                .balance(BigDecimal.valueOf(100.00))
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findByCustomerIdAndType(customerId, AccountType.CHECKING))
                .thenReturn(Optional.of(account));

        AccountInfo accountInfo = accountService.getMyAccountByType(customerId, AccountType.CHECKING);

        assertNotNull(accountInfo);
        assertEquals(AccountType.CHECKING, accountInfo.type());
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException when my account by type not found")
    void shouldThrowExceptionWhenMyAccountByTypeNotFound() {
        Long customerId = 1L;
        when(accountRepository.findByCustomerIdAndType(customerId, AccountType.CHECKING))
                .thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> {
            accountService.getMyAccountByType(customerId, AccountType.CHECKING);
        });
    }

    // ==================== Update Tests ====================

    @Test
    @DisplayName("Should update account type")
    void shouldUpdateAccountType() {
        String accountNumber = "ACC0000000001";
        Account existingAccount = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber(accountNumber)
                .customerId(1L)
                .balance(BigDecimal.valueOf(100.00))
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        AccountUpdateTypeRequest request = new AccountUpdateTypeRequest(AccountType.SAVINGS);

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(existingAccount));
        when(accountRepository.existsByCustomerIdAndType(1L, AccountType.SAVINGS)).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = accountService.updateAccountType(accountNumber, request);

        assertNotNull(response);
        assertEquals(AccountType.SAVINGS, response.type());
    }

    @Test
    @DisplayName("Should throw AccountAlreadyExistsException when updating to existing type")
    void shouldThrowAccountAlreadyExistsExceptionWhenUpdatingToExistingType() {
        String accountNumber = "ACC0000000001";
        Account existingAccount = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber(accountNumber)
                .customerId(1L)
                .balance(BigDecimal.valueOf(100.00))
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        AccountUpdateTypeRequest request = new AccountUpdateTypeRequest(AccountType.SAVINGS);

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(existingAccount));
        when(accountRepository.existsByCustomerIdAndType(1L, AccountType.SAVINGS)).thenReturn(true);

        assertThrows(AccountAlreadyExistsException.class, () -> {
            accountService.updateAccountType(accountNumber, request);
        });
    }

    @Test
    @DisplayName("Should update account status")
    void shouldUpdateAccountStatus() {
        String accountNumber = "ACC0000000001";
        Account existingAccount = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber(accountNumber)
                .customerId(1L)
                .balance(BigDecimal.valueOf(100.00))
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        AccountUpdateStatusRequest request = new AccountUpdateStatusRequest(AccountStatus.SUSPENDED);

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(existingAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = accountService.updateAccountStatus(accountNumber, request);

        assertNotNull(response);
        assertEquals(AccountStatus.SUSPENDED, response.status());
    }

    @Test
    @DisplayName("Should suspend account")
    void shouldSuspendAccount() {
        String accountNumber = "ACC0000000001";
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber(accountNumber)
                .customerId(1L)
                .balance(BigDecimal.valueOf(100.00))
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = accountService.suspendAccount(accountNumber);

        assertNotNull(response);
        assertEquals(AccountStatus.SUSPENDED, response.status());
    }

    // ==================== Deposit Tests ====================

    @Test
    @DisplayName("Should perform deposit successfully")
    void shouldPerformDepositSuccessfully() {
        Long customerId = 1L;
        String accountNumber = "ACC0000000001";
        BigDecimal initialBalance = BigDecimal.valueOf(100.00);
        BigDecimal depositAmount = BigDecimal.valueOf(50.00);

        Account account = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber(accountNumber)
                .customerId(customerId)
                .balance(initialBalance)
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        DepositBalanceRequest request = new DepositBalanceRequest(accountNumber, depositAmount);

        when(accountRepository.findByAccountNumberForUpdate(accountNumber)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.performDeposit(customerId, request);

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());

        assertEquals(BigDecimal.valueOf(150.00), accountCaptor.getValue().getBalance());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for negative deposit amount")
    void shouldThrowIllegalArgumentExceptionForNegativeDepositAmount() {
        Long customerId = 1L;
        String accountNumber = "ACC0000000001";
        DepositBalanceRequest request = new DepositBalanceRequest(accountNumber, BigDecimal.valueOf(-50.00));

        assertThrows(IllegalArgumentException.class, () -> {
            accountService.performDeposit(customerId, request);
        });
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for zero deposit amount")
    void shouldThrowIllegalArgumentExceptionForZeroDepositAmount() {
        Long customerId = 1L;
        String accountNumber = "ACC0000000001";
        DepositBalanceRequest request = new DepositBalanceRequest(accountNumber, BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class, () -> {
            accountService.performDeposit(customerId, request);
        });
    }

    @Test
    @DisplayName("Should throw InactiveAccountException for deposit to inactive account")
    void shouldThrowInactiveAccountExceptionForInactiveAccount() {
        Long customerId = 1L;
        String accountNumber = "ACC0000000001";

        Account account = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber(accountNumber)
                .customerId(customerId)
                .balance(BigDecimal.valueOf(100.00))
                .type(AccountType.CHECKING)
                .status(AccountStatus.SUSPENDED)
                .build();

        DepositBalanceRequest request = new DepositBalanceRequest(accountNumber, BigDecimal.valueOf(50.00));

        when(accountRepository.findByAccountNumberForUpdate(accountNumber)).thenReturn(Optional.of(account));

        assertThrows(InactiveAccountException.class, () -> {
            accountService.performDeposit(customerId, request);
        });
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException when depositing to another customer's account")
    void shouldThrowAccountNotFoundExceptionWhenDepositingToAnotherCustomerAccount() {
        Long customerId = 1L;
        Long otherCustomerId = 2L;
        String accountNumber = "ACC0000000001";

        Account account = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber(accountNumber)
                .customerId(otherCustomerId)
                .balance(BigDecimal.valueOf(100.00))
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        DepositBalanceRequest request = new DepositBalanceRequest(accountNumber, BigDecimal.valueOf(50.00));

        when(accountRepository.findByAccountNumberForUpdate(accountNumber)).thenReturn(Optional.of(account));

        assertThrows(AccountNotFoundException.class, () -> {
            accountService.performDeposit(customerId, request);
        });
    }

    // ==================== Payment Tests ====================

    @Test
    @DisplayName("Should perform payment successfully")
    void shouldPerformPaymentSuccessfully() {
        Long customerId = 1L;
        String fromAccountNumber = "ACC0000000001";
        String toAccountNumber = "ACC0000000002";

        Account fromAccount = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber(fromAccountNumber)
                .customerId(customerId)
                .balance(BigDecimal.valueOf(100.00))
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        Account toAccount = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber(toAccountNumber)
                .customerId(2L)
                .balance(BigDecimal.valueOf(50.00))
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        PaymentBalanceRequest request = new PaymentBalanceRequest(
                fromAccountNumber, toAccountNumber, BigDecimal.valueOf(30.00));

        when(accountRepository.findByAccountNumberForUpdate(anyString()))
                .thenReturn(Optional.of(fromAccount))
                .thenReturn(Optional.of(toAccount));

        accountService.performPayment(customerId, request);

        verify(accountRepository, times(2)).save(any(Account.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for negative payment amount")
    void shouldThrowIllegalArgumentExceptionForNegativePaymentAmount() {
        Long customerId = 1L;
        PaymentBalanceRequest request = new PaymentBalanceRequest(
                "ACC0000000001", "ACC0000000002", BigDecimal.valueOf(-30.00));

        assertThrows(IllegalArgumentException.class, () -> {
            accountService.performPayment(customerId, request);
        });
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for same source and destination accounts")
    void shouldThrowIllegalArgumentExceptionForSameAccounts() {
        Long customerId = 1L;
        String accountNumber = "ACC0000000001";
        PaymentBalanceRequest request = new PaymentBalanceRequest(
                accountNumber, accountNumber, BigDecimal.valueOf(30.00));

        assertThrows(IllegalArgumentException.class, () -> {
            accountService.performPayment(customerId, request);
        });
    }

    @Test
    @DisplayName("Should throw InsufficientFundsAvailableException for insufficient balance")
    void shouldThrowInsufficientFundsAvailableException() {
        Long customerId = 1L;
        String fromAccountNumber = "ACC0000000001";
        String toAccountNumber = "ACC0000000002";

        Account fromAccount = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber(fromAccountNumber)
                .customerId(customerId)
                .balance(BigDecimal.valueOf(10.00))
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        Account toAccount = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber(toAccountNumber)
                .customerId(2L)
                .balance(BigDecimal.valueOf(50.00))
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        PaymentBalanceRequest request = new PaymentBalanceRequest(
                fromAccountNumber, toAccountNumber, BigDecimal.valueOf(30.00));

        when(accountRepository.findByAccountNumberForUpdate(anyString()))
                .thenReturn(Optional.of(fromAccount))
                .thenReturn(Optional.of(toAccount));

        assertThrows(InsufficientFundsAvailableException.class, () -> {
            accountService.performPayment(customerId, request);
        });
    }

    @Test
    @DisplayName("Should throw InactiveAccountException for payment from inactive account")
    void shouldThrowInactiveAccountExceptionForPayment() {
        Long customerId = 1L;
        String fromAccountNumber = "ACC0000000001";
        String toAccountNumber = "ACC0000000002";

        Account fromAccount = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber(fromAccountNumber)
                .customerId(customerId)
                .balance(BigDecimal.valueOf(100.00))
                .type(AccountType.CHECKING)
                .status(AccountStatus.SUSPENDED)
                .build();

        Account toAccount = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber(toAccountNumber)
                .customerId(2L)
                .balance(BigDecimal.valueOf(50.00))
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        PaymentBalanceRequest request = new PaymentBalanceRequest(
                fromAccountNumber, toAccountNumber, BigDecimal.valueOf(30.00));

        when(accountRepository.findByAccountNumberForUpdate(anyString()))
                .thenReturn(Optional.of(fromAccount))
                .thenReturn(Optional.of(toAccount));

        assertThrows(InactiveAccountException.class, () -> {
            accountService.performPayment(customerId, request);
        });
    }

    // ==================== Refund Tests ====================

    @Test
    @DisplayName("Should perform refund successfully")
    void shouldPerformRefundSuccessfully() {
        Long customerId = 2L;
        String fromAccountNumber = "ACC0000000001";
        String toAccountNumber = "ACC0000000002";

        Account fromAccount = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber(fromAccountNumber)
                .customerId(customerId)
                .balance(BigDecimal.valueOf(100.00))
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        Account toAccount = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber(toAccountNumber)
                .customerId(1L)
                .balance(BigDecimal.valueOf(50.00))
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        RefundBalanceRequest request = new RefundBalanceRequest(
                fromAccountNumber, toAccountNumber, BigDecimal.valueOf(30.00));

        when(accountRepository.findByAccountNumberForUpdate(anyString()))
                .thenReturn(Optional.of(fromAccount))
                .thenReturn(Optional.of(toAccount));

        accountService.performRefund(customerId, request);

        verify(accountRepository, times(2)).save(any(Account.class));
    }

    @Test
    @DisplayName("Should throw NegativeRefundAmountException for negative refund amount")
    void shouldThrowNegativeRefundAmountExceptionForNegativeRefund() {
        Long customerId = 1L;
        RefundBalanceRequest request = new RefundBalanceRequest(
                "ACC0000000001", "ACC0000000002", BigDecimal.valueOf(-30.00));

        assertThrows(NegativeRefundAmountException.class, () -> {
            accountService.performRefund(customerId, request);
        });
    }

    @Test
    @DisplayName("Should throw SameRefundAccountsException for same accounts")
    void shouldThrowSameRefundAccountsException() {
        Long customerId = 1L;
        String accountNumber = "ACC0000000001";
        RefundBalanceRequest request = new RefundBalanceRequest(
                accountNumber, accountNumber, BigDecimal.valueOf(30.00));

        assertThrows(SameRefundAccountsException.class, () -> {
            accountService.performRefund(customerId, request);
        });
    }

    // ==================== Check Tests ====================

    @Test
    @DisplayName("Should return true when account is active")
    void shouldReturnTrueWhenAccountIsActive() {
        String accountNumber = "ACC0000000001";
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber(accountNumber)
                .customerId(1L)
                .balance(BigDecimal.valueOf(100.00))
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        boolean isActive = accountService.isAccountActive(accountNumber);

        assertTrue(isActive);
    }

    @Test
    @DisplayName("Should return false when account is not active")
    void shouldReturnFalseWhenAccountIsNotActive() {
        String accountNumber = "ACC0000000001";
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber(accountNumber)
                .customerId(1L)
                .balance(BigDecimal.valueOf(100.00))
                .type(AccountType.CHECKING)
                .status(AccountStatus.SUSPENDED)
                .build();

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        boolean isActive = accountService.isAccountActive(accountNumber);

        assertFalse(isActive);
    }

    @Test
    @DisplayName("Should return true when customer has sufficient funds by type")
    void shouldReturnTrueWhenHasSufficientFundsByType() {
        Long customerId = 1L;
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("ACC0000000001")
                .customerId(customerId)
                .balance(BigDecimal.valueOf(100.00))
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findByCustomerIdAndType(customerId, AccountType.CHECKING))
                .thenReturn(Optional.of(account));

        boolean hasFunds = accountService.hasSufficientFundsByType(
                customerId, AccountType.CHECKING, BigDecimal.valueOf(50.00));

        assertTrue(hasFunds);
    }

    @Test
    @DisplayName("Should return false when customer has insufficient funds by type")
    void shouldReturnFalseWhenInsufficientFundsByType() {
        Long customerId = 1L;
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("ACC0000000001")
                .customerId(customerId)
                .balance(BigDecimal.valueOf(30.00))
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findByCustomerIdAndType(customerId, AccountType.CHECKING))
                .thenReturn(Optional.of(account));

        boolean hasFunds = accountService.hasSufficientFundsByType(
                customerId, AccountType.CHECKING, BigDecimal.valueOf(50.00));

        assertFalse(hasFunds);
    }

    @Test
    @DisplayName("Should return true when account has sufficient funds by account number")
    void shouldReturnTrueWhenHasSufficientFundsByAccountNumber() {
        String accountNumber = "ACC0000000001";
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber(accountNumber)
                .customerId(1L)
                .balance(BigDecimal.valueOf(100.00))
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        boolean hasFunds = accountService.hasSufficientFundsByAccountNumber(
                accountNumber, BigDecimal.valueOf(50.00));

        assertTrue(hasFunds);
    }

    @Test
    @DisplayName("Should return true when account is owned by customer")
    void shouldReturnTrueWhenAccountIsOwnedByCustomer() {
        Long customerId = 1L;
        String accountNumber = "ACC0000000001";
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber(accountNumber)
                .customerId(customerId)
                .balance(BigDecimal.valueOf(100.00))
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        boolean isOwned = accountService.isAccountOwnedByCustomer(customerId, accountNumber);

        assertTrue(isOwned);
    }

    @Test
    @DisplayName("Should return false when account is not owned by customer")
    void shouldReturnFalseWhenAccountIsNotOwnedByCustomer() {
        Long customerId = 1L;
        String accountNumber = "ACC0000000001";
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber(accountNumber)
                .customerId(2L)
                .balance(BigDecimal.valueOf(100.00))
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        boolean isOwned = accountService.isAccountOwnedByCustomer(customerId, accountNumber);

        assertFalse(isOwned);
    }

    // ==================== Deletion Tests ====================

    @Test
    @DisplayName("Should delete account by account number")
    void shouldDeleteAccountByAccountNumber() {
        String accountNumber = "ACC0000000001";
        doNothing().when(accountRepository).deleteByAccountNumber(accountNumber);

        accountService.deleteAccount(accountNumber);

        verify(accountRepository).deleteByAccountNumber(accountNumber);
    }

    @Test
    @DisplayName("Should get account status")
    void shouldGetAccountStatus() {
        String accountNumber = "ACC0000000001";
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber(accountNumber)
                .customerId(1L)
                .balance(BigDecimal.valueOf(100.00))
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        AccountStatus status = accountService.getAccountStatus(accountNumber);

        assertEquals(AccountStatus.ACTIVE, status);
    }

    @Test
    @DisplayName("Should get account balance by account number")
    void shouldGetAccountBalanceByAccountNumber() {
        String accountNumber = "ACC0000000001";
        BigDecimal expectedBalance = BigDecimal.valueOf(100.00);
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber(accountNumber)
                .customerId(1L)
                .balance(expectedBalance)
                .type(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        BigDecimal balance = accountService.getAccountBalanceByAccountNumber(accountNumber);

        assertEquals(expectedBalance, balance);
    }
}
