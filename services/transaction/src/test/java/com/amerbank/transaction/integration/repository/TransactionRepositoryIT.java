package com.amerbank.transaction.integration.repository;

import com.amerbank.transaction.model.Transaction;
import com.amerbank.transaction.model.TransactionStatus;
import com.amerbank.transaction.model.TransactionType;
import com.amerbank.transaction.repository.TransactionRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = "spring.cloud.config.enabled=false")
@Testcontainers
@ActiveProfiles("test")
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
public class TransactionRepositoryIT {

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
    private TransactionRepository transactionRepository;

    @Nested
    @DisplayName("Create Operations")
    class CreateTests {

        @Test
        @DisplayName("Should save transaction to database")
        void shouldSaveTransaction() {
            Transaction transaction = Transaction.builder()
                    .amount(new BigDecimal("100.00"))
                    .fromAccountNumber("FROM001")
                    .toAccountNumber("TO001")
                    .type(TransactionType.DEPOSIT)
                    .status(TransactionStatus.APPROVED)
                    .idempotencyKey(UUID.randomUUID().toString())
                    .build();

            Transaction savedTransaction = transactionRepository.save(transaction);

            assertNotNull(savedTransaction.getId());
            assertEquals(1, transactionRepository.count());
        }

        @Test
        @DisplayName("Should save transaction with all fields")
        void shouldSaveTransactionWithAllFields() {
            String idempotencyKey = UUID.randomUUID().toString();
            Transaction transaction = Transaction.builder()
                    .amount(new BigDecimal("500.50"))
                    .description("Test payment")
                    .fromAccountNumber("FROM002")
                    .toAccountNumber("TO002")
                    .type(TransactionType.PAYMENT)
                    .status(TransactionStatus.APPROVED)
                    .idempotencyKey(idempotencyKey)
                    .build();

            Transaction savedTransaction = transactionRepository.save(transaction);

            assertNotNull(savedTransaction.getId());
            assertEquals(new BigDecimal("500.50"), savedTransaction.getAmount());
            assertEquals("Test payment", savedTransaction.getDescription());
            assertEquals("FROM002", savedTransaction.getFromAccountNumber());
            assertEquals("TO002", savedTransaction.getToAccountNumber());
            assertEquals(TransactionType.PAYMENT, savedTransaction.getType());
            assertEquals(TransactionStatus.APPROVED, savedTransaction.getStatus());
        }
    }

    @Nested
    @DisplayName("Lookup Operations")
    class LookupTests {

        private Transaction savedTransaction;

        @BeforeEach
        void setUp() {
            savedTransaction = transactionRepository.save(Transaction.builder()
                    .amount(new BigDecimal("100.00"))
                    .fromAccountNumber("FROM001")
                    .toAccountNumber("TO001")
                    .type(TransactionType.DEPOSIT)
                    .status(TransactionStatus.APPROVED)
                    .idempotencyKey(UUID.randomUUID().toString())
                    .build()
            );
        }

        @Test
        @DisplayName("Should find transaction by id")
        void shouldFindTransactionById() {
            Optional<Transaction> found = transactionRepository.findById(savedTransaction.getId());

            assertTrue(found.isPresent());
            assertEquals(savedTransaction.getId(), found.get().getId());
        }

        @Test
        @DisplayName("Should find transaction by from account number")
        void shouldFindTransactionByFromAccountNumber() {
            List<Transaction> found = transactionRepository.findByFromAccountNumber("FROM001");

            assertFalse(found.isEmpty());
            assertEquals(1, found.size());
            assertEquals("FROM001", found.get(0).getFromAccountNumber());
        }

        @Test
        @DisplayName("Should find transaction by to account number")
        void shouldFindTransactionByToAccountNumber() {
            List<Transaction> found = transactionRepository.findByToAccountNumber("TO001");

            assertFalse(found.isEmpty());
            assertEquals(1, found.size());
            assertEquals("TO001", found.get(0).getToAccountNumber());
        }

        @Test
        @DisplayName("Should find transaction by from and to account number")
        void shouldFindTransactionByFromAndToAccountNumber() {
            List<Transaction> found = transactionRepository.findByFromAccountNumberAndToAccountNumber("FROM001", "TO001");

            assertFalse(found.isEmpty());
            assertEquals("FROM001", found.get(0).getFromAccountNumber());
            assertEquals("TO001", found.get(0).getToAccountNumber());
        }

        @Test
        @DisplayName("Should find transaction by status")
        void shouldFindTransactionByStatus() {
            List<Transaction> found = transactionRepository.findByStatus(TransactionStatus.APPROVED);

            assertFalse(found.isEmpty());
            assertTrue(found.stream().allMatch(t -> t.getStatus() == TransactionStatus.APPROVED));
        }

        @Test
        @DisplayName("Should find transaction by type")
        void shouldFindTransactionByType() {
            List<Transaction> found = transactionRepository.findByType(TransactionType.DEPOSIT);

            assertFalse(found.isEmpty());
            assertTrue(found.stream().allMatch(t -> t.getType() == TransactionType.DEPOSIT));
        }

        @Test
        @DisplayName("Should find transaction by from account and status")
        void shouldFindTransactionByFromAccountAndStatus() {
            List<Transaction> found = transactionRepository.findByFromAccountNumberAndStatus("FROM001", TransactionStatus.APPROVED);

            assertFalse(found.isEmpty());
            assertTrue(found.stream().allMatch(t -> 
                t.getFromAccountNumber().equals("FROM001") && t.getStatus() == TransactionStatus.APPROVED));
        }

        @Test
        @DisplayName("Should find transaction by from account and type")
        void shouldFindTransactionByFromAccountAndType() {
            List<Transaction> found = transactionRepository.findByFromAccountNumberAndType("FROM001", TransactionType.DEPOSIT);

            assertFalse(found.isEmpty());
            assertTrue(found.stream().allMatch(t -> 
                t.getFromAccountNumber().equals("FROM001") && t.getType() == TransactionType.DEPOSIT));
        }

        @Test
        @DisplayName("Should find transaction by idempotency key")
        void shouldFindTransactionByIdempotencyKey() {
            Transaction transaction = Transaction.builder()
                    .amount(new BigDecimal("200.00"))
                    .fromAccountNumber("FROM003")
                    .toAccountNumber("TO003")
                    .type(TransactionType.PAYMENT)
                    .status(TransactionStatus.APPROVED)
                    .idempotencyKey("unique-idempotency-key")
                    .build();
            transactionRepository.save(transaction);

            Optional<Transaction> found = transactionRepository.findByIdempotencyKey("unique-idempotency-key");

            assertTrue(found.isPresent());
            assertEquals("unique-idempotency-key", found.get().getIdempotencyKey());
        }

        @Test
        @DisplayName("Should return empty when transaction not found by id")
        void shouldReturnEmptyWhenTransactionNotFoundById() {
            Optional<Transaction> found = transactionRepository.findById(UUID.randomUUID());

            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("Should return empty when transaction not found by idempotency key")
        void shouldReturnEmptyWhenTransactionNotFoundByIdempotencyKey() {
            Optional<Transaction> found = transactionRepository.findByIdempotencyKey("non-existent-key");

            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("Should find transactions by from or to account number")
        void shouldFindTransactionsByFromOrToAccountNumber() {
            transactionRepository.save(Transaction.builder()
                    .amount(new BigDecimal("300.00"))
                    .fromAccountNumber("FROM002")
                    .toAccountNumber("TO002")
                    .type(TransactionType.PAYMENT)
                    .status(TransactionStatus.APPROVED)
                    .idempotencyKey(UUID.randomUUID().toString())
                    .build());

            List<Transaction> found = transactionRepository.findByFromAccountNumberOrToAccountNumber("FROM002", "TO002");

            assertFalse(found.isEmpty());
        }
    }

    @Nested
    @DisplayName("Existence Checking Operations")
    class ExistenceTests {

        private Transaction savedTransaction;

        @BeforeEach
        void setUp() {
            savedTransaction = transactionRepository.save(Transaction.builder()
                    .amount(new BigDecimal("100.00"))
                    .fromAccountNumber("FROM004")
                    .toAccountNumber("TO004")
                    .type(TransactionType.DEPOSIT)
                    .status(TransactionStatus.APPROVED)
                    .idempotencyKey(UUID.randomUUID().toString())
                    .build()
            );
        }

        @Test
        @DisplayName("Should return true when transaction exists by id")
        void shouldReturnTrueWhenTransactionExistsById() {
            boolean result = transactionRepository.existsById(savedTransaction.getId());

            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when transaction does not exist by id")
        void shouldReturnFalseWhenTransactionDoesNotExistById() {
            boolean exists = transactionRepository.existsById(UUID.randomUUID());

            assertFalse(exists);
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteTests {

        private Transaction savedTransaction;

        @BeforeEach
        void setUp() {
            savedTransaction = transactionRepository.save(Transaction.builder()
                    .amount(new BigDecimal("100.00"))
                    .fromAccountNumber("FROM005")
                    .toAccountNumber("TO005")
                    .type(TransactionType.DEPOSIT)
                    .status(TransactionStatus.APPROVED)
                    .idempotencyKey(UUID.randomUUID().toString())
                    .build()
            );
        }

        @Test
        @DisplayName("Should delete transaction")
        void shouldDeleteTransaction() {
            assertEquals(1, transactionRepository.count());

            transactionRepository.delete(savedTransaction);

            assertEquals(0, transactionRepository.count());
        }

        @Test
        @DisplayName("Should delete transaction by id")
        void shouldDeleteTransactionById() {
            assertEquals(1, transactionRepository.count());

            transactionRepository.deleteById(savedTransaction.getId());

            assertEquals(0, transactionRepository.count());
        }

        @Test
        @DisplayName("Should delete all transactions")
        void shouldDeleteAllTransactions() {
            transactionRepository.save(Transaction.builder()
                    .amount(new BigDecimal("200.00"))
                    .fromAccountNumber("FROM006")
                    .toAccountNumber("TO006")
                    .type(TransactionType.PAYMENT)
                    .status(TransactionStatus.APPROVED)
                    .idempotencyKey(UUID.randomUUID().toString())
                    .build());

            assertEquals(2, transactionRepository.count());

            transactionRepository.deleteAll();

            assertEquals(0, transactionRepository.count());
        }

        @Test
        @DisplayName("Should delete all in batch")
        void shouldDeleteAllInBatch() {
            transactionRepository.save(Transaction.builder()
                    .amount(new BigDecimal("200.00"))
                    .fromAccountNumber("FROM006")
                    .toAccountNumber("TO006")
                    .type(TransactionType.PAYMENT)
                    .status(TransactionStatus.APPROVED)
                    .idempotencyKey(UUID.randomUUID().toString())
                    .build());

            assertEquals(2, transactionRepository.count());

            transactionRepository.deleteAllInBatch();

            assertEquals(0, transactionRepository.count());
        }
    }

    @Nested
    @DisplayName("Count Operations")
    class CountTests {

        @BeforeEach
        void setUp() {
            transactionRepository.save(Transaction.builder()
                    .amount(new BigDecimal("100.00"))
                    .fromAccountNumber("FROM007")
                    .toAccountNumber("TO007")
                    .type(TransactionType.DEPOSIT)
                    .status(TransactionStatus.APPROVED)
                    .idempotencyKey(UUID.randomUUID().toString())
                    .build());

            transactionRepository.save(Transaction.builder()
                    .amount(new BigDecimal("200.00"))
                    .fromAccountNumber("FROM008")
                    .toAccountNumber("TO008")
                    .type(TransactionType.DEPOSIT)
                    .status(TransactionStatus.APPROVED)
                    .idempotencyKey(UUID.randomUUID().toString())
                    .build());

            transactionRepository.save(Transaction.builder()
                    .amount(new BigDecimal("300.00"))
                    .fromAccountNumber("FROM009")
                    .toAccountNumber("TO009")
                    .type(TransactionType.PAYMENT)
                    .status(TransactionStatus.APPROVED)
                    .idempotencyKey(UUID.randomUUID().toString())
                    .build());
        }

        @Test
        @DisplayName("Should count transactions by type")
        void shouldCountTransactionsByType() {
            long depositCount = transactionRepository.countByType(TransactionType.DEPOSIT);
            long paymentCount = transactionRepository.countByType(TransactionType.PAYMENT);

            assertEquals(2, depositCount);
            assertEquals(1, paymentCount);
        }

        @Test
        @DisplayName("Should count all transactions")
        void shouldCountAllTransactions() {
            assertEquals(3, transactionRepository.count());
        }
    }

    @Nested
    @DisplayName("Date Range Queries")
    class DateRangeTests {

        @Test
        @DisplayName("Should find transactions by date range")
        void shouldFindTransactionsByDateRange() {
            Transaction transaction = transactionRepository.save(Transaction.builder()
                    .amount(new BigDecimal("100.00"))
                    .fromAccountNumber("FROM010")
                    .toAccountNumber("TO010")
                    .type(TransactionType.DEPOSIT)
                    .status(TransactionStatus.APPROVED)
                    .idempotencyKey(UUID.randomUUID().toString())
                    .build());

            LocalDateTime now = LocalDateTime.now();
            List<Transaction> found = transactionRepository.findByCreatedAtBetween(
                    now.minusHours(1), now.plusHours(1));

            assertFalse(found.isEmpty());
            assertTrue(found.stream().anyMatch(t -> t.getId().equals(transaction.getId())));
        }
    }

    @Nested
    @DisplayName("Top Queries")
    class TopQueriesTests {

        @BeforeEach
        void setUp() {
            for (int i = 0; i < 10; i++) {
                transactionRepository.save(Transaction.builder()
                        .amount(new BigDecimal("100.00"))
                        .fromAccountNumber("FROM011")
                        .toAccountNumber("TO011")
                        .type(TransactionType.DEPOSIT)
                        .status(TransactionStatus.APPROVED)
                        .idempotencyKey(UUID.randomUUID().toString())
                        .build());
            }
        }

        @Test
        @DisplayName("Should find top 5 transactions by from account")
        void shouldFindTop5TransactionsByFromAccount() {
            List<Transaction> found = transactionRepository.findTop5ByFromAccountNumberOrderByCreatedAtDesc("FROM011");

            assertEquals(5, found.size());
        }
    }
}
