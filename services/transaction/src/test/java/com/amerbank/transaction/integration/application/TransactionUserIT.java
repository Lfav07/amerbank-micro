package com.amerbank.transaction.integration.application;

import com.amerbank.transaction.dto.*;
import com.amerbank.transaction.model.Transaction;
import com.amerbank.transaction.model.TransactionStatus;
import com.amerbank.transaction.model.TransactionType;
import com.amerbank.transaction.repository.TransactionRepository;
import com.amerbank.transaction.service.AccountServiceClient;
import com.amerbank.transaction.util.TestJwtFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.cloud.config.enabled=false",
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
@ActiveProfiles("test")
public class TransactionUserIT {

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

    @TestConfiguration
    static class JwtTestConfig extends TestJwtFactory {
    }

    @MockitoBean
    private AccountServiceClient accountServiceClient;

    @Autowired
    private TestJwtFactory testJwtFactory;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private TransactionRepository transactionRepository;

    @AfterEach
    void clearDatabase() {
        transactionRepository.deleteAllInBatch();
    }

    private UUID createTestTransaction(String fromAccount, String toAccount, TransactionType type, TransactionStatus status) {
        Transaction tx = Transaction.builder()
                .amount(new BigDecimal("100.00"))
                .fromAccountNumber(fromAccount)
                .toAccountNumber(toAccount)
                .type(type)
                .status(status)
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
        return transactionRepository.save(tx).getId();
    }

    // -------------------------------------------------------------------------
    // Get My Transactions
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Get My Transactions")
    class GetMyTransactionsTests {

        @Test
        @DisplayName("Should get my transactions with valid token")
        void shouldGetMyTransactions() {
            Long customerId = 1L;
            String accountNumber = "ACC0000000001";
            createTestTransaction(accountNumber, "ACC0000000002", TransactionType.DEPOSIT, TransactionStatus.APPROVED);

            when(accountServiceClient.isAccountOwned(customerId, accountNumber)).thenReturn(true);

            String token = testJwtFactory.generateCustomerUserToken(customerId, "user@test.com");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<List> response = testRestTemplate.exchange(
                    "/transaction/me?accountNumber=" + accountNumber,
                    HttpMethod.GET,
                    entity,
                    List.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("Should not get transactions without token")
        void shouldNotGetTransactionsWithoutToken() {
            HttpEntity<?> entity = new HttpEntity<>(new HttpHeaders());

            ResponseEntity<String> response = testRestTemplate.exchange(
                    "/transaction/me?accountNumber=ACC0000000001",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not get transactions with invalid token")
        void shouldNotGetTransactionsWithInvalidToken() {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth("invalidToken");
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = testRestTemplate.exchange(
                    "/transaction/me?accountNumber=ACC0000000001",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }

        @Test
        @DisplayName("Should get transactions by from account")
        void shouldGetTransactionsByFromAccount() {
            Long customerId = 1L;
            String fromAccount = "ACC0000000001";
            createTestTransaction(fromAccount, "ACC0000000002", TransactionType.PAYMENT, TransactionStatus.APPROVED);

            when(accountServiceClient.isAccountOwned(customerId, fromAccount)).thenReturn(true);

            String token = testJwtFactory.generateCustomerUserToken(customerId, "user@test.com");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<List> response = testRestTemplate.exchange(
                    "/transaction/me/from?fromAccountNumber=" + fromAccount,
                    HttpMethod.GET,
                    entity,
                    List.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("Should get transactions by to account")
        void shouldGetTransactionsByToAccount() {
            Long customerId = 1L;
            String toAccount = "ACC0000000002";
            createTestTransaction("ACC0000000001", toAccount, TransactionType.DEPOSIT, TransactionStatus.APPROVED);

            when(accountServiceClient.isAccountOwned(customerId, toAccount)).thenReturn(true);

            String token = testJwtFactory.generateCustomerUserToken(customerId, "user@test.com");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<List> response = testRestTemplate.exchange(
                    "/transaction/me/to?toAccountNumber=" + toAccount,
                    HttpMethod.GET,
                    entity,
                    List.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("Should get transactions by status")
        void shouldGetTransactionsByStatus() {
            Long customerId = 1L;
            String accountNumber = "ACC0000000001";
            createTestTransaction(accountNumber, "ACC0000000002", TransactionType.DEPOSIT, TransactionStatus.APPROVED);

            when(accountServiceClient.isAccountOwned(customerId, accountNumber)).thenReturn(true);

            String token = testJwtFactory.generateCustomerUserToken(customerId, "user@test.com");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<List> response = testRestTemplate.exchange(
                    "/transaction/me/status?accountNumber=" + accountNumber + "&status=APPROVED",
                    HttpMethod.GET,
                    entity,
                    List.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("Should get transactions by type")
        void shouldGetTransactionsByType() {
            Long customerId = 1L;
            String accountNumber = "ACC0000000001";
            createTestTransaction(accountNumber, "ACC0000000002", TransactionType.PAYMENT, TransactionStatus.APPROVED);

            when(accountServiceClient.isAccountOwned(customerId, accountNumber)).thenReturn(true);

            String token = testJwtFactory.generateCustomerUserToken(customerId, "user@test.com");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<List> response = testRestTemplate.exchange(
                    "/transaction/me/type?accountNumber=" + accountNumber + "&type=PAYMENT",
                    HttpMethod.GET,
                    entity,
                    List.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("Should get transactions by from and to account")
        void shouldGetTransactionsByFromAndToAccount() {
            Long customerId = 1L;
            String fromAccount = "ACC0000000001";
            String toAccount = "ACC0000000002";
            createTestTransaction(fromAccount, toAccount, TransactionType.PAYMENT, TransactionStatus.APPROVED);

            when(accountServiceClient.isAccountOwned(customerId, fromAccount)).thenReturn(true);

            String token = testJwtFactory.generateCustomerUserToken(customerId, "user@test.com");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<List> response = testRestTemplate.exchange(
                    "/transaction/me/transfer?fromAccountNumber=" + fromAccount + "&toAccountNumber=" + toAccount,
                    HttpMethod.GET,
                    entity,
                    List.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
        }
    }

    // -------------------------------------------------------------------------
    // Create Deposit
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Create Deposit")
    class CreateDepositTests {

        @Test
        @DisplayName("Should create deposit with valid token")
        void shouldCreateDeposit() {
            Long customerId = 1L;
            String idempotencyKey = "deposit-" + UUID.randomUUID();
            DepositTransactionRequest request = new DepositTransactionRequest(
                    new BigDecimal("100.00"),
                    "Test deposit",
                    "FROM001",
                    "TO001"
            );

            doNothing().when(accountServiceClient).deposit(any(), any(), any());

            String token = testJwtFactory.generateCustomerUserToken(customerId, "user@test.com");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.set("idempotency-key", idempotencyKey);
            HttpEntity<DepositTransactionRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<TransactionResponse> response = testRestTemplate.exchange(
                    "/transaction/deposit",
                    HttpMethod.POST,
                    entity,
                    TransactionResponse.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("Should not create deposit without idempotency key")
        void shouldNotCreateDepositWithoutIdempotencyKey() {
            Long customerId = 1L;
            DepositTransactionRequest request = new DepositTransactionRequest(
                    new BigDecimal("100.00"),
                    "Test deposit",
                    "FROM001",
                    "TO001"
            );

            String token = testJwtFactory.generateCustomerUserToken(customerId, "user@test.com");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<DepositTransactionRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = testRestTemplate.exchange(
                    "/transaction/deposit",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not create deposit with invalid token")
        void shouldNotCreateDepositWithInvalidToken() {
            String idempotencyKey = "deposit-" + UUID.randomUUID();
            DepositTransactionRequest request = new DepositTransactionRequest(
                    new BigDecimal("100.00"),
                    "Test deposit",
                    "FROM001",
                    "TO001"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth("invalidToken");
            HttpEntity<DepositTransactionRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = testRestTemplate.exchange(
                    "/transaction/deposit",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not create deposit without token")
        void shouldNotCreateDepositWithoutToken() {
            DepositTransactionRequest request = new DepositTransactionRequest(
                    new BigDecimal("100.00"),
                    "Test deposit",
                    "FROM001",
                    "TO001"
            );

            HttpEntity<DepositTransactionRequest> entity = new HttpEntity<>(request);

            ResponseEntity<String> response = testRestTemplate.exchange(
                    "/transaction/deposit",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    // -------------------------------------------------------------------------
    // Create Payment
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Create Payment")
    class CreatePaymentTests {

        @Test
        @DisplayName("Should create payment with valid token")
        void shouldCreatePayment() {
            Long customerId = 1L;
            String idempotencyKey = "payment-" + UUID.randomUUID();
            PaymentTransactionRequest request = new PaymentTransactionRequest(
                    new BigDecimal("50.00"),
                    "Test payment",
                    "FROM001",
                    "TO001"
            );

            doNothing().when(accountServiceClient).payment(any(), any(), any(), any());

            String token = testJwtFactory.generateCustomerUserToken(customerId, "user@test.com");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.set("idempotency-key", idempotencyKey);
            HttpEntity<PaymentTransactionRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<TransactionResponse> response = testRestTemplate.exchange(
                    "/transaction/payment",
                    HttpMethod.POST,
                    entity,
                    TransactionResponse.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("Should not create payment without idempotency key")
        void shouldNotCreatePaymentWithoutIdempotencyKey() {
            Long customerId = 1L;
            PaymentTransactionRequest request = new PaymentTransactionRequest(
                    new BigDecimal("50.00"),
                    "Test payment",
                    "FROM001",
                    "TO001"
            );

            String token = testJwtFactory.generateCustomerUserToken(customerId, "user@test.com");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<PaymentTransactionRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = testRestTemplate.exchange(
                    "/transaction/payment",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not create payment with invalid token")
        void shouldNotCreatePaymentWithInvalidToken() {
            String idempotencyKey = "payment-" + UUID.randomUUID();
            PaymentTransactionRequest request = new PaymentTransactionRequest(
                    new BigDecimal("50.00"),
                    "Test payment",
                    "FROM001",
                    "TO001"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth("invalidToken");
            HttpEntity<PaymentTransactionRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = testRestTemplate.exchange(
                    "/transaction/payment",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }
    }

    // -------------------------------------------------------------------------
    // Create Refund
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Create Refund")
    class CreateRefundTests {

        @Test
        @DisplayName("Should create refund with valid token")
        void shouldCreateRefund() {
            Long customerId = 1L;
            String idempotencyKey = "payment-" + UUID.randomUUID();
            PaymentTransactionRequest paymentRequest = new PaymentTransactionRequest(
                    new BigDecimal("50.00"),
                    "Test payment",
                    "FROM001",
                    "TO001"
            );

            doNothing().when(accountServiceClient).payment(any(), any(), any(), any());

            String token = testJwtFactory.generateCustomerUserToken(customerId, "user@test.com");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.set("idempotency-key", idempotencyKey);
            HttpEntity<PaymentTransactionRequest> paymentEntity = new HttpEntity<>(paymentRequest, headers);

           ResponseEntity<TransactionResponse> paymentResponse =  testRestTemplate.exchange(
                    "/transaction/payment",
                    HttpMethod.POST,
                    paymentEntity,
                    TransactionResponse.class
            );

            assertNotNull(paymentResponse.getBody());
            UUID transactionId = paymentResponse.getBody().id();
            RefundTransactionRequest request = new RefundTransactionRequest(transactionId);

            doNothing().when(accountServiceClient).refund(any(), any(), any(), any());

;

            HttpEntity<RefundTransactionRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<TransactionResponse> response = testRestTemplate.exchange(
                    "/transaction/refund",
                    HttpMethod.POST,
                    entity,
                    TransactionResponse.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not create refund without idempotency key")
        void shouldNotCreateRefundWithoutIdempotencyKey() {
            Long customerId = 1L;
            RefundTransactionRequest request = new RefundTransactionRequest(UUID.randomUUID());

            String token = testJwtFactory.generateCustomerUserToken(customerId, "user@test.com");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<RefundTransactionRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = testRestTemplate.exchange(
                    "/transaction/refund",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not create refund with invalid token")
        void shouldNotCreateRefundWithInvalidToken() {
            RefundTransactionRequest request = new RefundTransactionRequest(UUID.randomUUID());

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth("invalidToken");
            HttpEntity<RefundTransactionRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = testRestTemplate.exchange(
                    "/transaction/refund",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }
    }
}
