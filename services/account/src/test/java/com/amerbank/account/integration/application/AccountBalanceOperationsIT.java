package com.amerbank.account.integration.application;

import com.amerbank.account.dto.AccountBalanceInfo;
import com.amerbank.account.model.Account;
import com.amerbank.account.model.AccountStatus;
import com.amerbank.account.model.AccountType;
import com.amerbank.account.repository.AccountRepository;
import com.amerbank.account.util.TestJwtFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.cloud.config.enabled=false",
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
@ActiveProfiles("test")
public class AccountBalanceOperationsIT {

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

    @Autowired
    private TestJwtFactory testJwtFactory;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AccountRepository accountRepository;


    @Nested
    @DisplayName("Get All Balances")
    class GetAllBalancesTests {

        @AfterEach
        void clearDatabase() {
            accountRepository.deleteAllInBatch();
        }

        @Test
        @DisplayName("Should get all account balances")
        void shouldGetAllAccountBalances() {
            Long customerId = 1L;
            String email = "test@email.com";
            String endpoint = "/account/me/balances";

            Account account1 = Account.builder()
                    .accountNumber("ACCT0000000001")
                    .customerId(customerId)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            Account account2 = Account.builder()
                    .accountNumber("ACCT0000000002")
                    .customerId(customerId)
                    .balance(new BigDecimal("1000.00"))
                    .type(AccountType.SAVINGS)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(account1);
            accountRepository.save(account2);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateUserToken(customerId, email));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<AccountBalanceInfo[]> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.GET,
                    entity,
                    AccountBalanceInfo[].class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(2, response.getBody().length);
        }

        @Test
        @DisplayName("Should return not found when no accounts exist")
        void shouldReturnNotFoundWhenNoAccounts() {
            Long customerId = 2L;
            String email = "test@email.com";
            String endpoint = "/account/me/balances";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateUserToken(customerId, email));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not get balances when JWT is invalid")
        void shouldNotGetBalancesWhenInvalidJwt() {
            String endpoint = "/account/me/balances";
            String fakeToken = "FakeToken";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(fakeToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }
    }


    @Nested
    @DisplayName("Get Balance By Type")
    class GetBalanceByTypeTests {

        @AfterEach
        void clearDatabase() {
            accountRepository.deleteAllInBatch();
        }

        @Test
        @DisplayName("Should get balance by account type")
        void shouldGetBalanceByAccountType() {
            Long customerId = 3L;
            String email = "test@email.com";
            String endpoint = "/account/me/balance";
            AccountType accountType = AccountType.CHECKING;

            Account account = Account.builder()
                    .accountNumber("ACCT0000000003")
                    .customerId(customerId)
                    .balance(new BigDecimal("750.50"))
                    .type(accountType)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(account);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateUserToken(customerId, email));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<BigDecimal> response = restTemplate.exchange(
                    endpoint + "?type=" + accountType,
                    HttpMethod.GET,
                    entity,
                    BigDecimal.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(new BigDecimal("750.50"), response.getBody());
        }

        @Test
        @DisplayName("Should return not found when account type does not exist")
        void shouldReturnNotFoundWhenAccountTypeNotFound() {
            Long customerId = 4L;
            String email = "test@email.com";
            String endpoint = "/account/me/balance";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateUserToken(customerId, email));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint + "?type=CHECKING",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }


    @Nested
    @DisplayName("Has Sufficient Funds")
    class HasSufficientFundsTests {

        @AfterEach
        void clearDatabase() {
            accountRepository.deleteAllInBatch();
        }

        @Test
        @DisplayName("Should return true when sufficient funds available")
        void shouldReturnTrueWhenSufficientFunds() {
            Long customerId = 6L;
            String email = "test@email.com";
            String endpoint = "/account/me/has-funds";
            AccountType accountType = AccountType.CHECKING;
            BigDecimal amount = new BigDecimal("100.00");

            Account account = Account.builder()
                    .accountNumber("ACCT0000000004")
                    .customerId(customerId)
                    .balance(new BigDecimal("500.00"))
                    .type(accountType)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(account);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateUserToken(customerId, email));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Boolean> response = restTemplate.exchange(
                    endpoint + "?type=" + accountType + "&amount=" + amount,
                    HttpMethod.GET,
                    entity,
                    Boolean.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody());
        }

        @Test
        @DisplayName("Should return false when insufficient funds")
        void shouldReturnFalseWhenInsufficientFunds() {
            Long customerId = 7L;
            String email = "test@email.com";
            String endpoint = "/account/me/has-funds";
            AccountType accountType = AccountType.CHECKING;
            BigDecimal amount = new BigDecimal("1000.00");

            Account account = Account.builder()
                    .accountNumber("ACCT0000000005")
                    .customerId(customerId)
                    .balance(new BigDecimal("500.00"))
                    .type(accountType)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(account);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateUserToken(customerId, email));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Boolean> response = restTemplate.exchange(
                    endpoint + "?type=" + accountType + "&amount=" + amount,
                    HttpMethod.GET,
                    entity,
                    Boolean.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertFalse(response.getBody());
        }

        @Test
        @DisplayName("Should return true when amount equals balance")
        void shouldReturnTrueWhenAmountEqualsBalance() {
            Long customerId = 8L;
            String email = "test@email.com";
            String endpoint = "/account/me/has-funds";
            AccountType accountType = AccountType.SAVINGS;
            BigDecimal amount = new BigDecimal("1000.00");

            Account account = Account.builder()
                    .accountNumber("ACCT0000000006")
                    .customerId(customerId)
                    .balance(new BigDecimal("1000.00"))
                    .type(accountType)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(account);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateUserToken(customerId, email));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Boolean> response = restTemplate.exchange(
                    endpoint + "?type=" + accountType + "&amount=" + amount,
                    HttpMethod.GET,
                    entity,
                    Boolean.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody());
        }

        @Test
        @DisplayName("Should return false when account type does not exist")
        void shouldReturnFalseWhenAccountTypeNotFound() {
            Long customerId = 9L;
            String email = "test@email.com";
            String endpoint = "/account/me/has-funds";
            AccountType accountType = AccountType.CHECKING;
            BigDecimal amount = new BigDecimal("100.00");

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateUserToken(customerId, email));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint + "?type=" + accountType + "&amount=" + amount,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return Internal Server Error when amount is zero")
        void shouldReturnBadRequestWhenAmountIsZero() {
            Long customerId = 10L;
            String email = "test@email.com";
            String endpoint = "/account/me/has-funds";
            AccountType accountType = AccountType.CHECKING;
            BigDecimal amount = BigDecimal.ZERO;

            Account account = Account.builder()
                    .accountNumber("ACCT0000000007")
                    .customerId(customerId)
                    .balance(new BigDecimal("500.00"))
                    .type(accountType)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(account);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateUserToken(customerId, email));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint + "?type=" + accountType + "&amount=" + amount,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }
}
