package com.amerbank.transaction.integration.application;

import com.amerbank.transaction.dto.*;
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
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.cloud.config.enabled=false",
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
@ActiveProfiles("test")
public class TransactionAdminIT {

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
        com.amerbank.transaction.model.Transaction tx = com.amerbank.transaction.model.Transaction.builder()
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
    // Get Transaction By ID
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Get Transaction By ID")
    class GetTransactionByIdTests {

        @Test
        @DisplayName("Should get transaction by ID with admin token")
        void shouldGetTransactionById() {
            UUID transactionId = createTestTransaction("FROM001", "TO001", TransactionType.DEPOSIT, TransactionStatus.APPROVED);

            String token = testJwtFactory.generateAdminToken(1L, "admin@test.com");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<TransactionResponse> response = testRestTemplate.exchange(
                    "/transaction/admin/" + transactionId,
                    HttpMethod.GET,
                    entity,
                    TransactionResponse.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(transactionId, response.getBody().id());
        }

        @Test
        @DisplayName("Should return 404 for non-existent transaction ID")
        void shouldReturn404ForNonExistentTransactionId() {
            String token = testJwtFactory.generateAdminToken(1L, "admin@test.com");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<ErrorResponse> response = testRestTemplate.exchange(
                    "/transaction/admin/" + UUID.randomUUID(),
                    HttpMethod.GET,
                    entity,
                    ErrorResponse.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not get transaction by ID with regular user token")
        void shouldNotGetTransactionByIdWithUserToken() {
            UUID transactionId = createTestTransaction("FROM001", "TO001", TransactionType.DEPOSIT, TransactionStatus.APPROVED);

            String token = testJwtFactory.generateCustomerUserToken(1L, "user@test.com");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = testRestTemplate.exchange(
                    "/transaction/admin/" + transactionId,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not get transaction by ID without token")
        void shouldNotGetTransactionByIdWithoutToken() {
            UUID transactionId = createTestTransaction("FROM001", "TO001", TransactionType.DEPOSIT, TransactionStatus.APPROVED);

            HttpEntity<?> entity = new HttpEntity<>(new HttpHeaders());

            ResponseEntity<Map> response = testRestTemplate.exchange(
                    "/transaction/admin/" + transactionId,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not get transaction by ID with invalid token")
        void shouldNotGetTransactionByIdWithInvalidToken() {
            UUID transactionId = createTestTransaction("FROM001", "TO001", TransactionType.DEPOSIT, TransactionStatus.APPROVED);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth("invalidToken");
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = testRestTemplate.exchange(
                    "/transaction/admin/" + transactionId,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }
    }

    // -------------------------------------------------------------------------
    // Get Transactions By From Account
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Get Transactions By From Account")
    class GetTransactionsByFromAccountTests {

        @Test
        @DisplayName("Should get transactions by from account with admin token")
        void shouldGetTransactionsByFromAccount() {
            createTestTransaction("FROM001", "TO001", TransactionType.DEPOSIT, TransactionStatus.APPROVED);

            String token = testJwtFactory.generateAdminToken(1L, "admin@test.com");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<List> response = testRestTemplate.exchange(
                    "/transaction/admin?fromAccount=FROM001",
                    HttpMethod.GET,
                    entity,
                    List.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertFalse(response.getBody().isEmpty());
        }

        @Test
        @DisplayName("Should not get transactions by from account with user token")
        void shouldNotGetTransactionsByFromAccountWithUserToken() {
            String token = testJwtFactory.generateCustomerUserToken(1L, "user@test.com");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = testRestTemplate.exchange(
                    "/transaction/admin?fromAccount=FROM001",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    // -------------------------------------------------------------------------
    // Get Transactions By To Account
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Get Transactions By To Account")
    class GetTransactionsByToAccountTests {

        @Test
        @DisplayName("Should get transactions by to account with admin token")
        void shouldGetTransactionsByToAccount() {
            createTestTransaction("FROM001", "TO001", TransactionType.PAYMENT, TransactionStatus.APPROVED);

            String token = testJwtFactory.generateAdminToken(1L, "admin@test.com");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<List> response = testRestTemplate.exchange(
                    "/transaction/admin?toAccount=TO001",
                    HttpMethod.GET,
                    entity,
                    List.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertFalse(response.getBody().isEmpty());
        }

        @Test
        @DisplayName("Should not get transactions by to account with user token")
        void shouldNotGetTransactionsByToAccountWithUserToken() {
            String token = testJwtFactory.generateCustomerUserToken(1L, "user@test.com");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = testRestTemplate.exchange(
                    "/transaction/admin?toAccount=TO001",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    // -------------------------------------------------------------------------
    // Get Transactions By Status
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Get Transactions By Status")
    class GetTransactionsByStatusTests {

        @Test
        @DisplayName("Should get transactions by status with admin token")
        void shouldGetTransactionsByStatus() {
            createTestTransaction("FROM001", "TO001", TransactionType.DEPOSIT, TransactionStatus.APPROVED);

            String token = testJwtFactory.generateAdminToken(1L, "admin@test.com");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<List> response = testRestTemplate.exchange(
                    "/transaction/admin?status=APPROVED",
                    HttpMethod.GET,
                    entity,
                    List.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertFalse(response.getBody().isEmpty());
        }

        @Test
        @DisplayName("Should not get transactions by status with user token")
        void shouldNotGetTransactionsByStatusWithUserToken() {
            String token = testJwtFactory.generateCustomerUserToken(1L, "user@test.com");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = testRestTemplate.exchange(
                    "/transaction/admin?status=APPROVED",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    // -------------------------------------------------------------------------
    // Get Transactions By Type
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Get Transactions By Type")
    class GetTransactionsByTypeTests {

        @Test
        @DisplayName("Should get transactions by type with admin token")
        void shouldGetTransactionsByType() {
            createTestTransaction("FROM001", "TO001", TransactionType.PAYMENT, TransactionStatus.APPROVED);

            String token = testJwtFactory.generateAdminToken(1L, "admin@test.com");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<List> response = testRestTemplate.exchange(
                    "/transaction/admin?type=PAYMENT",
                    HttpMethod.GET,
                    entity,
                    List.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertFalse(response.getBody().isEmpty());
        }

        @Test
        @DisplayName("Should not get transactions by type with user token")
        void shouldNotGetTransactionsByTypeWithUserToken() {
            String token = testJwtFactory.generateCustomerUserToken(1L, "user@test.com");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<ErrorResponse> response = testRestTemplate.exchange(
                    "/transaction/admin?type=PAYMENT",
                    HttpMethod.GET,
                    entity,
                    ErrorResponse.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    // -------------------------------------------------------------------------
    // Get Transactions By From And To Account
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Get Transactions By From And To Account")
    class GetTransactionsByFromAndToAccountTests {

        @Test
        @DisplayName("Should get transactions by from and to account with admin token")
        void shouldGetTransactionsByFromAndToAccount() {
            createTestTransaction("FROM001", "TO001", TransactionType.PAYMENT, TransactionStatus.APPROVED);

            String token = testJwtFactory.generateAdminToken(1L, "admin@test.com");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<List> response = testRestTemplate.exchange(
                    "/transaction/admin?fromAccount=FROM001&toAccount=TO001",
                    HttpMethod.GET,
                    entity,
                    List.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertFalse(response.getBody().isEmpty());
        }

        @Test
        @DisplayName("Should not get transactions by from and to account with user token")
        void shouldNotGetTransactionsByFromAndToAccountWithUserToken() {
            String token = testJwtFactory.generateCustomerUserToken(1L, "user@test.com");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = testRestTemplate.exchange(
                    "/transaction/admin?fromAccount=FROM001&toAccount=TO001",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }
}
