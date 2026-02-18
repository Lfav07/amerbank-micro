package com.amerbank.transaction.integration.service;

import com.amerbank.transaction.dto.*;
import com.amerbank.transaction.exception.*;
import com.amerbank.transaction.model.Transaction;
import com.amerbank.transaction.model.TransactionStatus;
import com.amerbank.transaction.model.TransactionType;
import com.amerbank.transaction.repository.TransactionRepository;
import com.amerbank.transaction.service.AccountServiceClient;
import com.amerbank.transaction.service.TransactionService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.cloud.config.enabled=false")
@Testcontainers
@Transactional
@ActiveProfiles("test")
public class TransactionServiceIntegrationTests {

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

    @MockitoBean
    private AccountServiceClient accountServiceClient;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepository;

    // -------------------------------------------------------------------------
    // Retrieval Tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Retrieval")
    class RetrievalTests {

        private Transaction createTestTransaction(String fromAccount, String toAccount, TransactionType type, TransactionStatus status) {
            Transaction transaction = Transaction.builder()
                    .amount(new BigDecimal("100.00"))
                    .fromAccountNumber(fromAccount)
                    .toAccountNumber(toAccount)
                    .type(type)
                    .status(status)
                    .idempotencyKey(UUID.randomUUID().toString())
                    .build();
            return transactionRepository.save(transaction);
        }

        @Test
        @DisplayName("Should find transaction by ID")
        void shouldFindTransactionById() {
            Transaction tx = createTestTransaction("FROM001", "TO001", TransactionType.DEPOSIT, TransactionStatus.APPROVED);

            Transaction found = transactionService.findTransactionById(tx.getId());

            assertNotNull(found);
            assertEquals(tx.getId(), found.getId());
        }

        @Test
        @DisplayName("Should throw TransactionNotFoundException when transaction not found by ID")
        void shouldThrowExceptionWhenTransactionNotFoundById() {
            assertThrows(TransactionNotFoundException.class,
                    () -> transactionService.findTransactionById(UUID.randomUUID()));
        }

        @Test
        @DisplayName("Should find transactions by from account number")
        void shouldFindTransactionsByFromAccountNumber() {
            createTestTransaction("FROM002", "TO002", TransactionType.PAYMENT, TransactionStatus.APPROVED);

            List<Transaction> found = transactionService.findTransactionsByFromAccountNumber("FROM002");

            assertFalse(found.isEmpty());
            assertTrue(found.stream().allMatch(t -> t.getFromAccountNumber().equals("FROM002")));
        }

        @Test
        @DisplayName("Should find transactions by to account number")
        void shouldFindTransactionsByToAccountNumber() {
            createTestTransaction("FROM003", "TO003", TransactionType.DEPOSIT, TransactionStatus.APPROVED);

            List<Transaction> found = transactionService.findTransactionsByToAccountNumber("TO003");

            assertFalse(found.isEmpty());
            assertTrue(found.stream().allMatch(t -> t.getToAccountNumber().equals("TO003")));
        }

        @Test
        @DisplayName("Should find transactions by from and to account number")
        void shouldFindTransactionsByFromAndToAccountNumber() {
            createTestTransaction("FROM004", "TO004", TransactionType.PAYMENT, TransactionStatus.APPROVED);

            List<Transaction> found = transactionService.findTransactionsByFromAndToAccountNumber("FROM004", "TO004");

            assertFalse(found.isEmpty());
        }

        @Test
        @DisplayName("Should find transactions by status")
        void shouldFindTransactionsByStatus() {
            createTestTransaction("FROM005", "TO005", TransactionType.DEPOSIT, TransactionStatus.APPROVED);

            List<Transaction> found = transactionService.findTransactionsByStatus(TransactionStatus.APPROVED);

            assertFalse(found.isEmpty());
            assertTrue(found.stream().allMatch(t -> t.getStatus() == TransactionStatus.APPROVED));
        }

        @Test
        @DisplayName("Should find transactions by type")
        void shouldFindTransactionsByType() {
            createTestTransaction("FROM006", "TO006", TransactionType.DEPOSIT, TransactionStatus.APPROVED);

            List<Transaction> found = transactionService.findTransactionsByType(TransactionType.DEPOSIT);

            assertFalse(found.isEmpty());
            assertTrue(found.stream().allMatch(t -> t.getType() == TransactionType.DEPOSIT));
        }

        @Test
        @DisplayName("Should return transaction response by ID")
        void shouldReturnTransactionResponseById() {
            Transaction tx = createTestTransaction("FROM007", "TO007", TransactionType.PAYMENT, TransactionStatus.APPROVED);

            TransactionResponse response = transactionService.getTransactionResponseById(tx.getId());

            assertNotNull(response);
            assertEquals(tx.getId(), response.id());
        }

        @Test
        @DisplayName("Should return transaction responses by from account")
        void shouldReturnTransactionResponsesByFromAccount() {
            createTestTransaction("FROM008", "TO008", TransactionType.DEPOSIT, TransactionStatus.APPROVED);

            List<TransactionResponse> responses = transactionService.getTransactionResponsesByFromAccountNumber("FROM008");

            assertFalse(responses.isEmpty());
        }

        @Test
        @DisplayName("Should return transaction responses by to account")
        void shouldReturnTransactionResponsesByToAccount() {
            createTestTransaction("FROM009", "TO009", TransactionType.PAYMENT, TransactionStatus.APPROVED);

            List<TransactionResponse> responses = transactionService.getTransactionResponsesByToAccountNumber("TO009");

            assertFalse(responses.isEmpty());
        }

        @Test
        @DisplayName("Should return transaction responses by status")
        void shouldReturnTransactionResponsesByStatus() {
            createTestTransaction("FROM010", "TO010", TransactionType.REFUND, TransactionStatus.APPROVED);

            List<TransactionResponse> responses = transactionService.getTransactionResponsesByStatus(TransactionStatus.APPROVED);

            assertFalse(responses.isEmpty());
        }

        @Test
        @DisplayName("Should return transaction responses by type")
        void shouldReturnTransactionResponsesByType() {
            createTestTransaction("FROM011", "TO011", TransactionType.WITHDRAWAL, TransactionStatus.APPROVED);

            List<TransactionResponse> responses = transactionService.getTransactionResponsesByType(TransactionType.WITHDRAWAL);

            assertFalse(responses.isEmpty());
        }
    }

    // -------------------------------------------------------------------------
    // Idempotency Tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Idempotency")
    class IdempotencyTests {

        @Test
        @DisplayName("Should create transaction with idempotency key")
        void shouldCreateTransactionWithIdempotencyKey() {
            String idempotencyKey = "test-idempotency-key-1";
            Long customerId = 1L;
            DepositTransactionRequest request = new DepositTransactionRequest(
                    new BigDecimal("100.00"),
                    "Test deposit",
                    "FROM012",
                    "TO012"
            );

            TransactionResponse response = transactionService.createDepositTransaction(customerId, idempotencyKey, request);

            assertNotNull(response);
            assertNotNull(response.id());
        }

        @Test
        @DisplayName("Should return same transaction for same idempotency key")
        void shouldReturnSameTransactionForSameIdempotencyKey() {
            String idempotencyKey = "test-idempotency-key-2";
            Long customerId = 1L;
            DepositTransactionRequest request = new DepositTransactionRequest(
                    new BigDecimal("200.00"),
                    "Test deposit",
                    "FROM013",
                    "TO013"
            );

            TransactionResponse response1 = transactionService.createDepositTransaction(customerId, idempotencyKey, request);
            TransactionResponse response2 = transactionService.createDepositTransaction(customerId, idempotencyKey, request);

            assertEquals(response1.id(), response2.id());
        }

        @Test
        @DisplayName("Should create different transactions for different idempotency keys")
        void shouldCreateDifferentTransactionsForDifferentIdempotencyKeys() {
            Long customerId = 1L;
            DepositTransactionRequest request = new DepositTransactionRequest(
                    new BigDecimal("300.00"),
                    "Test deposit",
                    "FROM014",
                    "TO014"
            );

            TransactionResponse response1 = transactionService.createDepositTransaction(customerId, "idempotency-key-3", request);
            TransactionResponse response2 = transactionService.createDepositTransaction(customerId, "idempotency-key-4", request);

            assertNotEquals(response1.id(), response2.id());
        }
    }

    // -------------------------------------------------------------------------
    // Payment Transaction Tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Payment Transactions")
    class PaymentTransactionTests {

        @Test
        @DisplayName("Should create payment transaction")
        void shouldCreatePaymentTransaction() {
            String idempotencyKey = "payment-key-1";
            Long customerId = 1L;
            PaymentTransactionRequest request = new PaymentTransactionRequest(
                    new BigDecimal("50.00"),
                    "Test payment",
                    "FROM015",
                    "TO015"
            );

            TransactionResponse response = transactionService.createPaymentTransaction(customerId, idempotencyKey, request);

            assertNotNull(response);
            assertNotNull(response.id());
            assertEquals(TransactionType.PAYMENT, response.type());
        }

        @Test
        @DisplayName("Should create payment with idempotency")
        void shouldCreatePaymentWithIdempotency() {
            String idempotencyKey = "payment-key-2";
            Long customerId = 1L;
            PaymentTransactionRequest request = new PaymentTransactionRequest(
                    new BigDecimal("75.00"),
                    "Test payment",
                    "FROM016",
                    "TO016"
            );

            TransactionResponse response1 = transactionService.createPaymentTransaction(customerId, idempotencyKey, request);
            TransactionResponse response2 = transactionService.createPaymentTransaction(customerId, idempotencyKey, request);

            assertEquals(response1.id(), response2.id());
        }
    }

    // -------------------------------------------------------------------------
    // Refund Transaction Tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Refund Transactions")
    class RefundTransactionTests {

        private Transaction createOriginalTransaction() {
            Transaction original = Transaction.builder()
                    .amount(new BigDecimal("100.00"))
                    .fromAccountNumber("FROM017")
                    .toAccountNumber("TO017")
                    .type(TransactionType.PAYMENT)
                    .status(TransactionStatus.APPROVED)
                    .idempotencyKey("original-tx-key")
                    .build();
            return transactionRepository.save(original);
        }

        @Test
        @DisplayName("Should create refund transaction")
        void shouldCreateRefundTransaction() {
            Transaction original = createOriginalTransaction();
            String idempotencyKey = "refund-key-1";
            Long customerId = 1L;
            RefundTransactionRequest request = new RefundTransactionRequest(original.getId());

            TransactionResponse response = transactionService.createRefundTransaction(customerId, idempotencyKey, request);

            assertNotNull(response);
            assertEquals(TransactionType.REFUND, response.type());
            assertEquals(original.getAmount(), response.amount());
        }

        @Test
        @DisplayName("Should mark original transaction as reversed after refund")
        void shouldMarkOriginalTransactionAsReversed() {
            Transaction original = createOriginalTransaction();
            String idempotencyKey = "refund-key-2";
            Long customerId = 1L;
            RefundTransactionRequest request = new RefundTransactionRequest(original.getId());

            transactionService.createRefundTransaction(customerId, idempotencyKey, request);

            Transaction updated = transactionRepository.findById(original.getId()).orElseThrow();
            assertEquals(TransactionStatus.REVERSED, updated.getStatus());
        }

        @Test
        @DisplayName("Should throw TransactionNotFoundException when refunding non-existent transaction")
        void shouldThrowExceptionWhenRefundingNonExistentTransaction() {
            String idempotencyKey = "refund-key-3";
            Long customerId = 1L;
            RefundTransactionRequest request = new RefundTransactionRequest(UUID.randomUUID());

            assertThrows(TransactionNotFoundException.class,
                    () -> transactionService.createRefundTransaction(customerId, idempotencyKey, request));
        }

        @Test
        @DisplayName("Should throw TransactionAlreadyRefundedException when transaction already refunded")
        void shouldThrowExceptionWhenTransactionAlreadyRefunded() {
            Transaction original = createOriginalTransaction();
            
            original.setStatus(TransactionStatus.REVERSED);
            transactionRepository.save(original);

            String idempotencyKey = "refund-key-4";
            Long customerId = 1L;
            RefundTransactionRequest request = new RefundTransactionRequest(original.getId());

            assertThrows(TransactionAlreadyRefundedException.class,
                    () -> transactionService.createRefundTransaction(customerId, idempotencyKey, request));
        }
    }

    // -------------------------------------------------------------------------
    // My Transactions Tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("My Transactions")
    class MyTransactionsTests {

        @Test
        @DisplayName("Should get my transactions when account is owned")
        void shouldGetMyTransactionsWhenAccountIsOwned() {
            Transaction tx = Transaction.builder()
                    .amount(new BigDecimal("100.00"))
                    .fromAccountNumber("ACC0000000001")
                    .toAccountNumber("ACC0000000002")
                    .type(TransactionType.DEPOSIT)
                    .status(TransactionStatus.APPROVED)
                    .idempotencyKey(UUID.randomUUID().toString())
                    .build();
            transactionRepository.save(tx);

            when(accountServiceClient.isAccountOwned(1L, "ACC0000000001")).thenReturn(true);

            List<Transaction> result = transactionService.getMyTransactions(1L, "ACC0000000001");

            assertFalse(result.isEmpty());
            assertEquals("ACC0000000001", result.get(0).getFromAccountNumber());
        }

        @Test
        @DisplayName("Should throw exception when account is not owned")
        void shouldThrowExceptionWhenAccountIsNotOwned() {
            when(accountServiceClient.isAccountOwned(1L, "ACC0000000001")).thenReturn(false);

            assertThrows(UnauthorizedAccountAccessException.class,
                    () -> transactionService.getMyTransactions(1L, "ACC0000000001"));
        }

        @Test
        @DisplayName("Should throw exception when account service is unavailable")
        void shouldThrowExceptionWhenAccountServiceUnavailable() {
            when(accountServiceClient.isAccountOwned(1L, "ACC0000000001"))
                    .thenThrow(new AccountServiceUnavailableException("Service unavailable"));

            assertThrows(AccountServiceUnavailableException.class,
                    () -> transactionService.getMyTransactions(1L, "ACC0000000001"));
        }
    }
}
