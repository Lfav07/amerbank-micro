package com.amerbank.account.integration.application;

import com.amerbank.account.dto.*;
import com.amerbank.account.model.Account;
import com.amerbank.account.model.AccountStatus;
import com.amerbank.account.model.AccountType;
import com.amerbank.account.repository.AccountRepository;
import com.amerbank.account.util.TestJwtFactory;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.cloud.config.enabled=false",
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
@ActiveProfiles("test")
public class InternalAccountIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    @Container
    static RedisContainer redisContainer = new RedisContainer("redis:6.2.6").withExposedPorts(6379);



    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
    }

    @TestConfiguration
    static class JwtTestConfig extends TestJwtFactory {
    }

    @Autowired
    private TestJwtFactory testJwtFactory;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AccountRepository accountRepository;


    @Nested
    @DisplayName("Is Account Owned")
    class IsAccountOwnedTests {

        @AfterEach
        void clearDatabase() {
            accountRepository.deleteAllInBatch();
        }

        @Test
        @DisplayName("Should return true when account belongs to customer")
        void shouldReturnTrueWhenAccountBelongsToCustomer() {
            Long customerId = 1L;
            String accountNumber = "ACCT0000000001";
            String endpoint = "/account/internal/owned";

            Account account = Account.builder()
                    .accountNumber(accountNumber)
                    .customerId(customerId)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(account);

            ServiceAccountOwnedRequest request = new ServiceAccountOwnedRequest(customerId, accountNumber);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ServiceAccountOwnedRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Boolean> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    Boolean.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody());
        }

        @Test
        @DisplayName("Should return false when account does not belong to customer")
        void shouldReturnFalseWhenAccountDoesNotBelongToCustomer() {
            Long customerId = 1L;
            Long differentCustomerId = 2L;
            String accountNumber = "ACCT0000000002";
            String endpoint = "/account/internal/owned";

            Account account = Account.builder()
                    .accountNumber(accountNumber)
                    .customerId(differentCustomerId)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(account);

            ServiceAccountOwnedRequest request = new ServiceAccountOwnedRequest(customerId, accountNumber);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ServiceAccountOwnedRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Boolean> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    Boolean.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertFalse(response.getBody());
        }

        @Test
        @DisplayName("Should return false when customer id is null")
        void shouldReturnFalseWhenCustomerIdIsNull() {
            String accountNumber = "ACCT0000000003";
            String endpoint = "/account/internal/owned";

            Account account = Account.builder()
                    .accountNumber(accountNumber)
                    .customerId(1L)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(account);

            ServiceAccountOwnedRequest request = new ServiceAccountOwnedRequest(null, accountNumber);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ServiceAccountOwnedRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Boolean> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    Boolean.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertFalse(response.getBody());
        }

        @Test
        @DisplayName("Should return 404 when account not found")
        void shouldThrowExceptionWhenAccountNotFound() {
            Long customerId = 1L;
            String endpoint = "/account/internal/owned";

            ServiceAccountOwnedRequest request = new ServiceAccountOwnedRequest(customerId, "ACCT9999999999");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ServiceAccountOwnedRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    ErrorResponse.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Account not found", response.getBody().getMessage());
            assertEquals(404, response.getBody().getStatus());
        }
    }


    @Nested
    @DisplayName("Deposit")
    class DepositTests {

        @AfterEach
        void clearDatabase() {
            accountRepository.deleteAllInBatch();
        }

        @Test
        @DisplayName("Should deposit successfully")
        void shouldDepositSuccessfully() {
            Long customerId = 1L;
            String accountNumber = "ACCT0000000010";
            BigDecimal initialBalance = new BigDecimal("500.00");
            BigDecimal depositAmount = new BigDecimal("100.00");
            String endpoint = "/account/internal/deposit";

            Account account = Account.builder()
                    .accountNumber(accountNumber)
                    .customerId(customerId)
                    .balance(initialBalance)
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(account);

            ServiceDepositBalanceRequest request = new ServiceDepositBalanceRequest(customerId, accountNumber, depositAmount);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ServiceDepositBalanceRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Void> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    Void.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());

            Account updated = accountRepository.findByAccountNumber(accountNumber).orElseThrow();
            assertEquals(initialBalance.add(depositAmount), updated.getBalance());
        }

        @Test
        @DisplayName("Should return bad request when amount is zero")
        void shouldReturnBadRequestWhenAmountIsZero() {
            Long customerId = 1L;
            String accountNumber = "ACCT0000000011";
            String endpoint = "/account/internal/deposit";

            Account account = Account.builder()
                    .accountNumber(accountNumber)
                    .customerId(customerId)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(account);

            ServiceDepositBalanceRequest request = new ServiceDepositBalanceRequest(customerId, accountNumber, BigDecimal.ZERO);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ServiceDepositBalanceRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return bad request when amount is negative")
        void shouldReturnBadRequestWhenAmountIsNegative() {
            Long customerId = 1L;
            String accountNumber = "ACCT0000000012";
            String endpoint = "/account/internal/deposit";

            Account account = Account.builder()
                    .accountNumber(accountNumber)
                    .customerId(customerId)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(account);

            ServiceDepositBalanceRequest request = new ServiceDepositBalanceRequest(customerId, accountNumber, new BigDecimal("-100.00"));
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ServiceDepositBalanceRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return not found when account not found")
        void shouldReturnNotFoundWhenAccountNotFound() {
            Long customerId = 1L;
            String endpoint = "/account/internal/deposit";

            ServiceDepositBalanceRequest request = new ServiceDepositBalanceRequest(customerId, "ACCT9999999999", new BigDecimal("100.00"));
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ServiceDepositBalanceRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return not found when account not owned by customer")
        void shouldReturnNotFoundWhenAccountNotOwnedByCustomer() {
            Long customerId = 1L;
            Long differentCustomerId = 2L;
            String accountNumber = "ACCT0000000013";
            String endpoint = "/account/internal/deposit";

            Account account = Account.builder()
                    .accountNumber(accountNumber)
                    .customerId(differentCustomerId)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(account);

            ServiceDepositBalanceRequest request = new ServiceDepositBalanceRequest(customerId, accountNumber, new BigDecimal("100.00"));
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ServiceDepositBalanceRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return bad request when account is suspended")
        void shouldReturnBadRequestWhenAccountIsSuspended() {
            Long customerId = 1L;
            String accountNumber = "ACCT0000000014";
            String endpoint = "/account/internal/deposit";

            Account account = Account.builder()
                    .accountNumber(accountNumber)
                    .customerId(customerId)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.SUSPENDED)
                    .build();
            accountRepository.save(account);

            ServiceDepositBalanceRequest request = new ServiceDepositBalanceRequest(customerId, accountNumber, new BigDecimal("100.00"));
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ServiceDepositBalanceRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }


    @Nested
    @DisplayName("Payment")
    class PaymentTests {

        @AfterEach
        void clearDatabase() {
            accountRepository.deleteAllInBatch();
        }

        @Test
        @DisplayName("Should transfer successfully")
        void shouldTransferSuccessfully() {
            Long customerId = 1L;
            String fromAccountNumber = "ACCT0000000020";
            String toAccountNumber = "ACCT0000000021";
            BigDecimal initialBalance = new BigDecimal("500.00");
            BigDecimal transferAmount = new BigDecimal("100.00");
            String endpoint = "/account/internal/payment";

            Account fromAccount = Account.builder()
                    .accountNumber(fromAccountNumber)
                    .customerId(customerId)
                    .balance(initialBalance)
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            Account toAccount = Account.builder()
                    .accountNumber(toAccountNumber)
                    .customerId(customerId)
                    .balance(initialBalance)
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            ServicePaymentRequest request = new ServicePaymentRequest(customerId, fromAccountNumber, toAccountNumber, transferAmount);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ServicePaymentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Void> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    Void.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());

            Account updatedFrom = accountRepository.findByAccountNumber(fromAccountNumber).orElseThrow();
            Account updatedTo = accountRepository.findByAccountNumber(toAccountNumber).orElseThrow();
            assertEquals(initialBalance.subtract(transferAmount), updatedFrom.getBalance());
            assertEquals(initialBalance.add(transferAmount), updatedTo.getBalance());
        }

        @Test
        @DisplayName("Should return bad request when amount is zero")
        void shouldReturnBadRequestWhenAmountIsZero() {
            Long customerId = 1L;
            String fromAccountNumber = "ACCT0000000022";
            String toAccountNumber = "ACCT0000000023";
            String endpoint = "/account/internal/payment";

            Account fromAccount = Account.builder()
                    .accountNumber(fromAccountNumber)
                    .customerId(customerId)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            Account toAccount = Account.builder()
                    .accountNumber(toAccountNumber)
                    .customerId(customerId)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            ServicePaymentRequest request = new ServicePaymentRequest(customerId, fromAccountNumber, toAccountNumber, BigDecimal.ZERO);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ServicePaymentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return bad request when source and destination are same")
        void shouldReturnBadRequestWhenSourceAndDestinationAreSame() {
            Long customerId = 1L;
            String accountNumber = "ACCT0000000024";
            String endpoint = "/account/internal/payment";

            Account account = Account.builder()
                    .accountNumber(accountNumber)
                    .customerId(customerId)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(account);

            ServicePaymentRequest request = new ServicePaymentRequest(customerId, accountNumber, accountNumber, new BigDecimal("100.00"));
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ServicePaymentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return bad request when insufficient funds")
        void shouldReturnBadRequestWhenInsufficientFunds() {
            Long customerId = 1L;
            String fromAccountNumber = "ACCT0000000025";
            String toAccountNumber = "ACCT0000000026";
            BigDecimal initialBalance = new BigDecimal("50.00");
            BigDecimal transferAmount = new BigDecimal("100.00");
            String endpoint = "/account/internal/payment";

            Account fromAccount = Account.builder()
                    .accountNumber(fromAccountNumber)
                    .customerId(customerId)
                    .balance(initialBalance)
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            Account toAccount = Account.builder()
                    .accountNumber(toAccountNumber)
                    .customerId(customerId)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            ServicePaymentRequest request = new ServicePaymentRequest(customerId, fromAccountNumber, toAccountNumber, transferAmount);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ServicePaymentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return not found when source account not found")
        void shouldReturnNotFoundWhenSourceAccountNotFound() {
            Long customerId = 1L;
            String endpoint = "/account/internal/payment";

            ServicePaymentRequest request = new ServicePaymentRequest(customerId, "ACCT9999999999", "ACCT0000000027", new BigDecimal("100.00"));
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ServicePaymentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return not found when source account not owned by customer")
        void shouldReturnNotFoundWhenSourceAccountNotOwnedByCustomer() {
            Long customerId = 1L;
            Long differentCustomerId = 2L;
            String fromAccountNumber = "ACCT0000000028";
            String toAccountNumber = "ACCT0000000029";
            String endpoint = "/account/internal/payment";

            Account fromAccount = Account.builder()
                    .accountNumber(fromAccountNumber)
                    .customerId(differentCustomerId)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            Account toAccount = Account.builder()
                    .accountNumber(toAccountNumber)
                    .customerId(customerId)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            ServicePaymentRequest request = new ServicePaymentRequest(customerId, fromAccountNumber, toAccountNumber, new BigDecimal("100.00"));
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ServicePaymentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return bad request when source account is suspended")
        void shouldReturnBadRequestWhenSourceAccountIsSuspended() {
            Long customerId = 1L;
            String fromAccountNumber = "ACCT0000000030";
            String toAccountNumber = "ACCT0000000031";
            String endpoint = "/account/internal/payment";

            Account fromAccount = Account.builder()
                    .accountNumber(fromAccountNumber)
                    .customerId(customerId)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.SUSPENDED)
                    .build();
            Account toAccount = Account.builder()
                    .accountNumber(toAccountNumber)
                    .customerId(customerId)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            ServicePaymentRequest request = new ServicePaymentRequest(customerId, fromAccountNumber, toAccountNumber, new BigDecimal("100.00"));
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ServicePaymentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return bad request when destination account is suspended")
        void shouldReturnBadRequestWhenDestinationAccountIsSuspended() {
            Long customerId = 1L;
            String fromAccountNumber = "ACCT0000000032";
            String toAccountNumber = "ACCT0000000033";
            String endpoint = "/account/internal/payment";

            Account fromAccount = Account.builder()
                    .accountNumber(fromAccountNumber)
                    .customerId(customerId)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            Account toAccount = Account.builder()
                    .accountNumber(toAccountNumber)
                    .customerId(customerId)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.SUSPENDED)
                    .build();
            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            ServicePaymentRequest request = new ServicePaymentRequest(customerId, fromAccountNumber, toAccountNumber, new BigDecimal("100.00"));
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ServicePaymentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }


    @Nested
    @DisplayName("Refund")
    class RefundTests {

        @AfterEach
        void clearDatabase() {
            accountRepository.deleteAllInBatch();
        }

        @Test
        @DisplayName("Should refund successfully")
        void shouldRefundSuccessfully() {
            Long customerId = 1L;
            String fromAccountNumber = "ACCT0000000040";
            String toAccountNumber = "ACCT0000000041";
            BigDecimal initialBalance = new BigDecimal("500.00");
            BigDecimal refundAmount = new BigDecimal("100.00");
            String endpoint = "/account/internal/refund";

            Account fromAccount = Account.builder()
                    .accountNumber(fromAccountNumber)
                    .customerId(customerId)
                    .balance(initialBalance)
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            Account toAccount = Account.builder()
                    .accountNumber(toAccountNumber)
                    .customerId(customerId)
                    .balance(initialBalance)
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            ServiceRefundBalanceRequest request = new ServiceRefundBalanceRequest(customerId, fromAccountNumber, toAccountNumber, refundAmount);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ServiceRefundBalanceRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Void> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    Void.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());

            Account updatedFrom = accountRepository.findByAccountNumber(fromAccountNumber).orElseThrow();
            Account updatedTo = accountRepository.findByAccountNumber(toAccountNumber).orElseThrow();
            assertEquals(initialBalance.subtract(refundAmount), updatedFrom.getBalance());
            assertEquals(initialBalance.add(refundAmount), updatedTo.getBalance());
        }

        @Test
        @DisplayName("Should return bad request when amount is zero")
        void shouldReturnBadRequestWhenAmountIsZero() {
            Long customerId = 1L;
            String fromAccountNumber = "ACCT0000000042";
            String toAccountNumber = "ACCT0000000043";
            String endpoint = "/account/internal/refund";

            Account fromAccount = Account.builder()
                    .accountNumber(fromAccountNumber)
                    .customerId(customerId)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            Account toAccount = Account.builder()
                    .accountNumber(toAccountNumber)
                    .customerId(customerId)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            ServiceRefundBalanceRequest request = new ServiceRefundBalanceRequest(customerId, fromAccountNumber, toAccountNumber, BigDecimal.ZERO);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ServiceRefundBalanceRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return bad request when amount is negative")
        void shouldReturnBadRequestWhenAmountIsNegative() {
            Long customerId = 1L;
            String fromAccountNumber = "ACCT0000000044";
            String toAccountNumber = "ACCT0000000045";
            String endpoint = "/account/internal/refund";

            Account fromAccount = Account.builder()
                    .accountNumber(fromAccountNumber)
                    .customerId(customerId)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            Account toAccount = Account.builder()
                    .accountNumber(toAccountNumber)
                    .customerId(customerId)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            ServiceRefundBalanceRequest request = new ServiceRefundBalanceRequest(customerId, fromAccountNumber, toAccountNumber, new BigDecimal("-100.00"));
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ServiceRefundBalanceRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return bad request when source and destination are same")
        void shouldReturnBadRequestWhenSourceAndDestinationAreSame() {
            Long customerId = 1L;
            String accountNumber = "ACCT0000000046";
            String endpoint = "/account/internal/refund";

            Account account = Account.builder()
                    .accountNumber(accountNumber)
                    .customerId(customerId)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(account);

            ServiceRefundBalanceRequest request = new ServiceRefundBalanceRequest(customerId, accountNumber, accountNumber, new BigDecimal("100.00"));
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ServiceRefundBalanceRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return bad request when insufficient funds")
        void shouldReturnBadRequestWhenInsufficientFunds() {
            Long customerId = 1L;
            String fromAccountNumber = "ACCT0000000047";
            String toAccountNumber = "ACCT0000000048";
            BigDecimal initialBalance = new BigDecimal("50.00");
            BigDecimal refundAmount = new BigDecimal("100.00");
            String endpoint = "/account/internal/refund";

            Account fromAccount = Account.builder()
                    .accountNumber(fromAccountNumber)
                    .customerId(customerId)
                    .balance(initialBalance)
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            Account toAccount = Account.builder()
                    .accountNumber(toAccountNumber)
                    .customerId(customerId)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            ServiceRefundBalanceRequest request = new ServiceRefundBalanceRequest(customerId, fromAccountNumber, toAccountNumber, refundAmount);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ServiceRefundBalanceRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return not found when source account not found")
        void shouldReturnNotFoundWhenSourceAccountNotFound() {
            Long customerId = 1L;
            String endpoint = "/account/internal/refund";

            ServiceRefundBalanceRequest request = new ServiceRefundBalanceRequest(customerId, "ACCT9999999999", "ACCT0000000049", new BigDecimal("100.00"));
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ServiceRefundBalanceRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return not found when source account not owned by customer")
        void shouldReturnNotFoundWhenSourceAccountNotOwnedByCustomer() {
            Long customerId = 1L;
            Long differentCustomerId = 2L;
            String fromAccountNumber = "ACCT0000000050";
            String toAccountNumber = "ACCT0000000051";
            String endpoint = "/account/internal/refund";

            Account fromAccount = Account.builder()
                    .accountNumber(fromAccountNumber)
                    .customerId(differentCustomerId)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            Account toAccount = Account.builder()
                    .accountNumber(toAccountNumber)
                    .customerId(customerId)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            ServiceRefundBalanceRequest request = new ServiceRefundBalanceRequest(customerId, fromAccountNumber, toAccountNumber, new BigDecimal("100.00"));
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ServiceRefundBalanceRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return bad request when source account is suspended")
        void shouldReturnBadRequestWhenSourceAccountIsSuspended() {
            Long customerId = 1L;
            String fromAccountNumber = "ACCT0000000052";
            String toAccountNumber = "ACCT0000000053";
            String endpoint = "/account/internal/refund";

            Account fromAccount = Account.builder()
                    .accountNumber(fromAccountNumber)
                    .customerId(customerId)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.SUSPENDED)
                    .build();
            Account toAccount = Account.builder()
                    .accountNumber(toAccountNumber)
                    .customerId(customerId)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            ServiceRefundBalanceRequest request = new ServiceRefundBalanceRequest(customerId, fromAccountNumber, toAccountNumber, new BigDecimal("100.00"));
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ServiceRefundBalanceRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return bad request when destination account is suspended")
        void shouldReturnBadRequestWhenDestinationAccountIsSuspended() {
            Long customerId = 1L;
            String fromAccountNumber = "ACCT0000000054";
            String toAccountNumber = "ACCT0000000055";
            String endpoint = "/account/internal/refund";

            Account fromAccount = Account.builder()
                    .accountNumber(fromAccountNumber)
                    .customerId(customerId)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            Account toAccount = Account.builder()
                    .accountNumber(toAccountNumber)
                    .customerId(customerId)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.SUSPENDED)
                    .build();
            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            ServiceRefundBalanceRequest request = new ServiceRefundBalanceRequest(customerId, fromAccountNumber, toAccountNumber, new BigDecimal("100.00"));
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ServiceRefundBalanceRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }
}
