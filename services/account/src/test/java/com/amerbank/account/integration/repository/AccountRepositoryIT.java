package com.amerbank.account.integration.repository;

import com.amerbank.account.model.Account;
import com.amerbank.account.model.AccountStatus;
import com.amerbank.account.model.AccountType;
import com.amerbank.account.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = "spring.cloud.config.enabled=false")
@Testcontainers
@ActiveProfiles("test")
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
public class AccountRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private AccountRepository accountRepository;

    @Nested
    @DisplayName("Lookup Operations")
    class LookupTests {

        Account savedAccount;

        @BeforeEach
        void setUp() {
            savedAccount = accountRepository.save(Account.builder()
                    .accountNumber("ACC0000000001")
                    .customerId(1L)
                    .balance(BigDecimal.valueOf(100.00))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build()
            );
        }

        @Test
        @DisplayName("Should find account by account number")
        void shouldFindAccountByAccountNumber() {
            Optional<Account> found = accountRepository.findByAccountNumber(savedAccount.getAccountNumber());

            assertTrue(found.isPresent());
            assertEquals(savedAccount.getAccountNumber(), found.get().getAccountNumber());
        }

        @Test
        @DisplayName("Should find account by customer ID")
        void shouldFindAccountByCustomerId() {
            List<Account> found = accountRepository.findAllByCustomerId(savedAccount.getCustomerId());

            assertFalse(found.isEmpty());
            assertEquals(savedAccount.getCustomerId(), found.get(0).getCustomerId());
        }

        @Test
        @DisplayName("Should find account by customer ID and type")
        void shouldFindAccountByCustomerIdAndType() {
            Optional<Account> found = accountRepository.findByCustomerIdAndType(
                    savedAccount.getCustomerId(), AccountType.CHECKING);

            assertTrue(found.isPresent());
            assertEquals(AccountType.CHECKING, found.get().getType());
        }

        @Test
        @DisplayName("Should return empty when account not found by account number")
        void shouldReturnEmptyWhenAccountNotFoundByAccountNumber() {
            Optional<Account> found = accountRepository.findByAccountNumber("ACC9999999999");

            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("Should return empty list when no accounts found by customer ID")
        void shouldReturnEmptyListWhenNoAccountsFoundByCustomerId() {
            List<Account> found = accountRepository.findAllByCustomerId(9999L);

            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("Should return empty when account not found by customer ID and type")
        void shouldReturnEmptyWhenAccountNotFoundByCustomerIdAndType() {
            Optional<Account> found = accountRepository.findByCustomerIdAndType(
                    9999L, AccountType.SAVINGS);

            assertTrue(found.isEmpty());
        }
    }

    @Nested
    @DisplayName("Existence Checking Operations")
    class ExistenceTests {

        Account savedAccount;

        @BeforeEach
        void setUp() {
            savedAccount = accountRepository.save(Account.builder()
                    .accountNumber("ACC0000000002")
                    .customerId(2L)
                    .balance(BigDecimal.valueOf(200.00))
                    .type(AccountType.SAVINGS)
                    .status(AccountStatus.ACTIVE)
                    .build()
            );
        }

        @Test
        @DisplayName("Should return true when account exists by account number")
        void shouldReturnTrueWhenAccountExistsByAccountNumber() {
            boolean result = accountRepository.existsByAccountNumber(savedAccount.getAccountNumber());

            assertTrue(result);
        }

        @Test
        @DisplayName("Should return true when account exists by customer ID and type")
        void shouldReturnTrueWhenAccountExistsByCustomerIdAndType() {
            boolean result = accountRepository.existsByCustomerIdAndType(
                    savedAccount.getCustomerId(), AccountType.SAVINGS);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when account does not exist by account number")
        void shouldReturnFalseWhenAccountDoesNotExistByAccountNumber() {
            boolean exists = accountRepository.existsByAccountNumber("ACC9999999999");

            assertFalse(exists);
        }

        @Test
        @DisplayName("Should return false when account does not exist by customer ID and type")
        void shouldReturnFalseWhenAccountDoesNotExistByCustomerIdAndType() {
            boolean exists = accountRepository.existsByCustomerIdAndType(9999L, AccountType.CHECKING);

            assertFalse(exists);
        }
    }

    @Nested
    @DisplayName("Create Operations")
    class CreateTests {

        @Test
        @DisplayName("Should save account to database")
        void shouldSaveAccount() {
            Account account = Account.builder()
                    .accountNumber("ACC0000000003")
                    .customerId(3L)
                    .balance(BigDecimal.valueOf(300.00))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();

            Account savedAccount = accountRepository.save(account);

            assertNotNull(savedAccount.getId());
            assertEquals(1, accountRepository.count());
        }

        @Test
        @DisplayName("Should save multiple accounts for same customer with different types")
        void shouldSaveMultipleAccountsForSameCustomerWithDifferentTypes() {
            Account checkingAccount = Account.builder()
                    .accountNumber("ACC0000000004")
                    .customerId(4L)
                    .balance(BigDecimal.valueOf(100.00))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();

            Account savingsAccount = Account.builder()
                    .accountNumber("ACC0000000005")
                    .customerId(4L)
                    .balance(BigDecimal.valueOf(500.00))
                    .type(AccountType.SAVINGS)
                    .status(AccountStatus.ACTIVE)
                    .build();

            accountRepository.save(checkingAccount);
            accountRepository.save(savingsAccount);

            assertEquals(2, accountRepository.count());
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteTests {

        Account savedAccount;

        @BeforeEach
        void setUp() {
            savedAccount = accountRepository.save(Account.builder()
                    .accountNumber("ACC0000000006")
                    .customerId(5L)
                    .balance(BigDecimal.valueOf(600.00))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build()
            );
        }

        @Test
        @DisplayName("Should delete account by account number")
        void shouldDeleteAccountByAccountNumber() {
            assertEquals(1, accountRepository.count());

            accountRepository.deleteByAccountNumber(savedAccount.getAccountNumber());

            assertEquals(0, accountRepository.count());
        }

        @Test
        @DisplayName("Should delete account")
        void shouldDeleteAccount() {
            assertEquals(1, accountRepository.count());

            accountRepository.delete(savedAccount);

            assertEquals(0, accountRepository.count());
        }

        @Test
        @DisplayName("Should delete account by ID")
        void shouldDeleteAccountById() {
            assertEquals(1, accountRepository.count());

            accountRepository.deleteById(savedAccount.getId());

            assertEquals(0, accountRepository.count());
        }

        @Test
        @DisplayName("Should delete all accounts")
        void shouldDeleteAllAccounts() {
            accountRepository.save(Account.builder()
                    .accountNumber("ACC0000000007")
                    .customerId(6L)
                    .balance(BigDecimal.valueOf(700.00))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build()
            );

            assertEquals(2, accountRepository.count());

            accountRepository.deleteAll();

            assertEquals(0, accountRepository.count());
        }
    }

    @Nested
    @DisplayName("Update Operations")
    class UpdateTests {

        Account savedAccount;

        @BeforeEach
        void setUp() {
            savedAccount = accountRepository.save(Account.builder()
                    .accountNumber("ACC0000000008")
                    .customerId(7L)
                    .balance(BigDecimal.valueOf(800.00))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build()
            );
        }

        @Test
        @DisplayName("Should update account status")
        void shouldUpdateAccountStatus() {
            savedAccount.setStatus(AccountStatus.SUSPENDED);
            Account updated = accountRepository.save(savedAccount);

            assertEquals(AccountStatus.SUSPENDED, updated.getStatus());
        }

        @Test
        @DisplayName("Should update account type")
        void shouldUpdateAccountType() {
            savedAccount.setType(AccountType.SAVINGS);
            Account updated = accountRepository.save(savedAccount);

            assertEquals(AccountType.SAVINGS, updated.getType());
        }

        @Test
        @DisplayName("Should update account balance")
        void shouldUpdateAccountBalance() {
            savedAccount.setBalance(BigDecimal.valueOf(1500.00));
            Account updated = accountRepository.save(savedAccount);

            assertEquals(BigDecimal.valueOf(1500.00), updated.getBalance());
        }
    }

    @Nested
    @DisplayName("Pessimistic Lock Tests")
    class PessimisticLockTests {

        @Test
        @DisplayName("Should find account by account number for update")
        void shouldFindAccountByAccountNumberForUpdate() {
            Account account = accountRepository.save(Account.builder()
                    .accountNumber("ACC0000000009")
                    .customerId(8L)
                    .balance(BigDecimal.valueOf(900.00))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build()
            );

            Optional<Account> found = accountRepository.findByAccountNumberForUpdate(account.getAccountNumber());

            assertTrue(found.isPresent());
            assertEquals(account.getAccountNumber(), found.get().getAccountNumber());
        }

        @Test
        @DisplayName("Should return empty when account not found for update")
        void shouldReturnEmptyWhenAccountNotFoundForUpdate() {
            Optional<Account> found = accountRepository.findByAccountNumberForUpdate("ACC9999999999");

            assertTrue(found.isEmpty());
        }
    }
}
