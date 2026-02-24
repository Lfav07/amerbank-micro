package com.amerbank.account.integration.application;

import com.amerbank.account.dto.AccountInfo;
import com.amerbank.account.dto.AccountRequest;
import com.amerbank.account.dto.AccountResponse;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.cloud.config.enabled=false",
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
@ActiveProfiles("test")
public class AccountRegistrationIT {

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
    @DisplayName("Account Registration")
    class AccountRegistrationTests {

        @AfterEach
        void clearDatabase() {
            accountRepository.deleteAllInBatch();
        }

        @Test
        @DisplayName("Should register account successfully")
        void shouldRegisterAccount() {
            Long customerId = 1L;
            String email = "test@email.com";
            AccountType accountType = AccountType.CHECKING;
            String endpoint = "/account/register";

            AccountRequest request = new AccountRequest(accountType);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateUserToken(customerId, email));
            HttpEntity<AccountRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    (Class<Map<String, String>>) (Class<?>) Map.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Account successfully registered", response.getBody().get("message"));

            List<com.amerbank.account.model.Account> accounts = accountRepository.findAllByCustomerId(customerId);
            assertFalse(accounts.isEmpty());
            assertEquals(accountType, accounts.get(0).getType());
        }

        @Test
        @DisplayName("Should register account with initial balance")
        void shouldRegisterAccountWithInitialBalance() {
            Long customerId = 2L;
            String email = "test@email.com";
            AccountType accountType = AccountType.SAVINGS;
            String endpoint = "/account/register";

            AccountRequest request = new AccountRequest(accountType);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateUserToken(customerId, email));
            HttpEntity<AccountRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    (Class<Map<String, String>>) (Class<?>) Map.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatusCode());

            List<com.amerbank.account.model.Account> accounts = accountRepository.findAllByCustomerId(customerId);
            assertFalse(accounts.isEmpty());
            assertEquals(new BigDecimal("1000.00"), accounts.get(0).getBalance());
        }

        @Test
        @DisplayName("Should not register account when type already exists")
        void shouldNotRegisterAccountWhenTypeAlreadyExists() {
            Long customerId = 3L;
            String email = "test@email.com";
            AccountType accountType = AccountType.CHECKING;
            String endpoint = "/account/register";

            AccountRequest request = new AccountRequest(accountType);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateUserToken(customerId, email));
            HttpEntity<AccountRequest> entity = new HttpEntity<>(request, headers);

            restTemplate.exchange(endpoint, HttpMethod.POST, entity, (Class<Map<String, String>>) (Class<?>) Map.class);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not register account when JWT is invalid")
        void shouldNotRegisterAccountWhenInvalidJwt() {
            String endpoint = "/account/register";
            String fakeToken = "FakeToken";

            AccountRequest request = new AccountRequest(AccountType.CHECKING);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(fakeToken);
            HttpEntity<AccountRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not register account when no JWT provided")
        void shouldNotRegisterAccountWhenNoJwt() {
            String endpoint = "/account/register";

            AccountRequest request = new AccountRequest(AccountType.CHECKING);
            HttpEntity<AccountRequest> entity = new HttpEntity<>(request);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not register account when account type is null")
        void shouldNotRegisterAccountWhenTypeIsNull() {
            Long customerId = 4L;
            String email = "test@email.com";
            String endpoint = "/account/register";

            AccountRequest request = new AccountRequest(null);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateUserToken(customerId, email));
            HttpEntity<AccountRequest> entity = new HttpEntity<>(request, headers);

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
    @DisplayName("Get My Accounts")
    class GetMyAccountsTests {

        @AfterEach
        void clearDatabase() {
            accountRepository.deleteAllInBatch();
        }

        @Test
        @DisplayName("Should get my accounts successfully")
        void shouldGetMyAccounts() {
            Long customerId = 5L;
            String email = "test@email.com";
            String endpoint = "/account/me";

            com.amerbank.account.model.Account account = com.amerbank.account.model.Account.builder()
                    .accountNumber("ACCT0000000001")
                    .customerId(customerId)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(com.amerbank.account.model.AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(account);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateUserToken(customerId, email));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<AccountInfo[]> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.GET,
                    entity,
                    AccountInfo[].class
            );

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().length);
            assertEquals("ACCT0000000001", response.getBody()[0].accountNumber());
        }

        @Test
        @DisplayName("Should return empty list when no accounts exist")
        void shouldReturnEmptyListWhenNoAccounts() {
            Long customerId = 6L;
            String email = "test@email.com";
            String endpoint = "/account/me";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateUserToken(customerId, email));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<AccountInfo[]> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.GET,
                    entity,
                    AccountInfo[].class
            );

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(0, response.getBody().length);
        }

        @Test
        @DisplayName("Should get account by type")
        void shouldGetAccountByType() {
            Long customerId = 7L;
            String email = "test@email.com";
            String endpoint = "/account/me/type";

            com.amerbank.account.model.Account account = com.amerbank.account.model.Account.builder()
                    .accountNumber("ACCT0000000002")
                    .customerId(customerId)
                    .balance(new BigDecimal("1000.00"))
                    .type(AccountType.SAVINGS)
                    .status(com.amerbank.account.model.AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(account);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateUserToken(customerId, email));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<AccountInfo> response = restTemplate.exchange(
                    endpoint + "?type=SAVINGS",
                    HttpMethod.GET,
                    entity,
                    AccountInfo.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(AccountType.SAVINGS, response.getBody().type());
        }

        @Test
        @DisplayName("Should return not found when account type does not exist")
        void shouldReturnNotFoundWhenAccountTypeNotFound() {
            Long customerId = 8L;
            String email = "test@email.com";
            String endpoint = "/account/me/type";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateUserToken(customerId, email));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<?> response = restTemplate.exchange(
                    endpoint + "?type=CHECKING",
                    HttpMethod.GET,
                    entity,
                    Object.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not get accounts when JWT is invalid")
        void shouldNotGetAccountsWhenInvalidJwt() {
            String endpoint = "/account/me";
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
}
