package com.amerbank.account.integration.application;

import com.amerbank.account.dto.AccountResponse;
import com.amerbank.account.dto.AccountUpdateStatusRequest;
import com.amerbank.account.dto.AccountUpdateTypeRequest;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.cloud.config.enabled=false",
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
@ActiveProfiles("test")
public class AccountAdminIT {

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
    @DisplayName("Get Accounts")
    class GetAccountsTests {

        @AfterEach
        void clearDatabase() {
            accountRepository.deleteAllInBatch();
        }

        @Test
        @DisplayName("Should get accounts by customer ID")
        void shouldGetAccountsByCustomerId() {
            Long customerId = 1L;
            String endpoint = "/account/admin/customers/" + customerId;

            Account account = Account.builder()
                    .accountNumber("ACCT0000000001")
                    .customerId(customerId)
                    .balance(new BigDecimal("500.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(account);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateAdminToken("admin@amerbank.com"));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<AccountResponse[]> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.GET,
                    entity,
                    AccountResponse[].class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().length);
            assertEquals(customerId, response.getBody()[0].customerId());
        }

        @Test
        @DisplayName("Should return empty array when no accounts for customer")
        void shouldReturnEmptyArrayWhenNoAccounts() {
            Long customerId = 999L;
            String endpoint = "/account/admin/customers/" + customerId;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateAdminToken("admin@amerbank.com"));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<AccountResponse[]> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.GET,
                    entity,
                    AccountResponse[].class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(0, response.getBody().length);
        }

        @Test
        @DisplayName("Should get account by customer ID and type")
        void shouldGetAccountByCustomerIdAndType() {
            Long customerId = 3L;
            AccountType type = AccountType.SAVINGS;
            String endpoint = "/account/admin/customers/" + customerId + "/type?type=" + type;

            Account account = Account.builder()
                    .accountNumber("ACCT0000000002")
                    .customerId(customerId)
                    .balance(new BigDecimal("1000.00"))
                    .type(type)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(account);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateAdminToken("admin@amerbank.com"));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<AccountResponse> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.GET,
                    entity,
                    AccountResponse.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(type, response.getBody().type());
        }

        @Test
        @DisplayName("Should return not found when account by customer and type not found")
        void shouldReturnNotFoundWhenAccountByCustomerAndTypeNotFound() {
            Long customerId = 4L;
            String endpoint = "/account/admin/customers/" + customerId + "/type?type=CHECKING";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateAdminToken("admin@amerbank.com"));
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
        @DisplayName("Should get account by account number")
        void shouldGetAccountByAccountNumber() {
            String accountNumber = "ACCT0000000003";
            String endpoint = "/account/admin/" + accountNumber;
            Long customerId = 5L;

            Account account = Account.builder()
                    .accountNumber(accountNumber)
                    .customerId(customerId)
                    .balance(new BigDecimal("750.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(account);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateAdminToken("admin@amerbank.com"));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<AccountResponse> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.GET,
                    entity,
                    AccountResponse.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(accountNumber, response.getBody().accountNumber());
            assertEquals(customerId, response.getBody().customerId());
        }

        @Test
        @DisplayName("Should return not found when account number not found")
        void shouldReturnNotFoundWhenAccountNumberNotFound() {
            String endpoint = "/account/admin/ACCT9999999999";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateAdminToken("admin@amerbank.com"));
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
        @DisplayName("Should return forbidden when user is not admin")
        void shouldReturnForbiddenWhenUserIsNotAdmin() {
            Long customerId = 6L;
            String endpoint = "/account/admin/customers/" + customerId;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateUserToken(customerId, "user@email.com"));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }


    @Nested
    @DisplayName("Update Account Type")
    class UpdateAccountTypeTests {

        @AfterEach
        void clearDatabase() {
            accountRepository.deleteAllInBatch();
        }

        @Test
        @DisplayName("Should update account type")
        void shouldUpdateAccountType() {
            String accountNumber = "ACCT0000000004";
            Long customerId = 7L;
            String endpoint = "/account/admin/" + accountNumber + "/type";

            Account account = Account.builder()
                    .accountNumber(accountNumber)
                    .customerId(customerId)
                    .balance(new BigDecimal("200.00"))
                    .type(AccountType.CHECKING)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(account);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateAdminToken("admin@amerbank.com"));
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            AccountUpdateTypeRequest request = new AccountUpdateTypeRequest(AccountType.SAVINGS);
            HttpEntity<AccountUpdateTypeRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<AccountResponse> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.PUT,
                    entity,
                    AccountResponse.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(AccountType.SAVINGS, response.getBody().type());
        }

        @Test
        @DisplayName("Should return conflict when account type already exists for customer")
        void shouldReturnConflictWhenAccountTypeAlreadyExists() {
            Long customerId = 8L;
            AccountType existingType = AccountType.CHECKING;
            String existingAccountNumber = "ACCT0000000005";
            
            Account existingAccount = Account.builder()
                    .accountNumber(existingAccountNumber)
                    .customerId(customerId)
                    .balance(new BigDecimal("300.00"))
                    .type(existingType)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(existingAccount);

            String targetAccountNumber = "ACCT0000000006";
            Account targetAccount = Account.builder()
                    .accountNumber(targetAccountNumber)
                    .customerId(customerId)
                    .balance(new BigDecimal("400.00"))
                    .type(AccountType.SAVINGS)
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(targetAccount);

            String endpoint = "/account/admin/" + targetAccountNumber + "/type";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateAdminToken("admin@amerbank.com"));
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            AccountUpdateTypeRequest request = new AccountUpdateTypeRequest(existingType);
            HttpEntity<AccountUpdateTypeRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.PUT,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return not found when account not found")
        void shouldReturnNotFoundWhenAccountNotFound() {
            String endpoint = "/account/admin/ACCT9999999999/type";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateAdminToken("admin@amerbank.com"));
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            AccountUpdateTypeRequest request = new AccountUpdateTypeRequest(AccountType.SAVINGS);
            HttpEntity<AccountUpdateTypeRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.PUT,
                    entity,
                    String.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }


        @Nested
        @DisplayName("Update Account Status")
        class UpdateAccountStatusTests {

            @AfterEach
            void clearDatabase() {
                accountRepository.deleteAllInBatch();
            }

            @Test
            @DisplayName("Should update account status")
            void shouldUpdateAccountStatus() {
                String accountNumber = "ACCT0000000007";
                Long customerId = 9L;
                String endpoint = "/account/admin/" + accountNumber + "/status";

                Account account = Account.builder()
                        .accountNumber(accountNumber)
                        .customerId(customerId)
                        .balance(new BigDecimal("500.00"))
                        .type(AccountType.CHECKING)
                        .status(AccountStatus.ACTIVE)
                        .build();
                accountRepository.save(account);

                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(testJwtFactory.generateAdminToken("admin@amerbank.com"));
                headers.setContentType(MediaType.APPLICATION_JSON);
                
                AccountUpdateStatusRequest request = new AccountUpdateStatusRequest(AccountStatus.CLOSED);
                HttpEntity<AccountUpdateStatusRequest> entity = new HttpEntity<>(request, headers);

                ResponseEntity<AccountResponse> response = restTemplate.exchange(
                        endpoint,
                        HttpMethod.PUT,
                        entity,
                        AccountResponse.class
                );

                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());
                assertEquals(AccountStatus.CLOSED, response.getBody().status());
            }


            @Nested
            @DisplayName("Suspend Account")
            class SuspendAccountTests {

                @AfterEach
                void clearDatabase() {
                    accountRepository.deleteAllInBatch();
                }

                @Test
                @DisplayName("Should suspend account")
                void shouldSuspendAccount() {
                    String accountNumber = "ACCT0000000008";
                    Long customerId = 10L;
                    String endpoint = "/account/admin/" + accountNumber + "/suspend";

                    Account account = Account.builder()
                            .accountNumber(accountNumber)
                            .customerId(customerId)
                            .balance(new BigDecimal("600.00"))
                            .type(AccountType.CHECKING)
                            .status(AccountStatus.ACTIVE)
                            .build();
                    accountRepository.save(account);

                    HttpHeaders headers = new HttpHeaders();
                    headers.setBearerAuth(testJwtFactory.generateAdminToken("admin@amerbank.com"));
                    HttpEntity<Void> entity = new HttpEntity<>(headers);

                    ResponseEntity<AccountResponse> response = restTemplate.exchange(
                            endpoint,
                            HttpMethod.PUT,
                            entity,
                            AccountResponse.class
                    );

                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(AccountStatus.SUSPENDED, response.getBody().status());
                }

                @Test
                @DisplayName("Should return not found when account not found")
                void shouldReturnNotFoundWhenAccountNotFound() {
                    String endpoint = "/account/admin/ACCT9999999999/suspend";

                    HttpHeaders headers = new HttpHeaders();
                    headers.setBearerAuth(testJwtFactory.generateAdminToken("admin@amerbank.com"));
                    HttpEntity<Void> entity = new HttpEntity<>(headers);

                    ResponseEntity<String> response = restTemplate.exchange(
                            endpoint,
                            HttpMethod.PUT,
                            entity,
                            String.class
                    );

                    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                }
            }


            @Nested
            @DisplayName("Delete Account")
            class DeleteAccountTests {

                @AfterEach
                void clearDatabase() {
                    accountRepository.deleteAllInBatch();
                }

                @Test
                @DisplayName("Should delete account")
                void shouldDeleteAccount() {
                    String accountNumber = "ACCT0000000012";
                    Long customerId = 11L;
                    String endpoint = "/account/admin/" + accountNumber + "?customerId=" + customerId;

                    Account account = Account.builder()
                            .accountNumber(accountNumber)
                            .customerId(customerId)
                            .balance(new BigDecimal("700.00"))
                            .type(AccountType.CHECKING)
                            .status(AccountStatus.ACTIVE)
                            .build();
                    accountRepository.save(account);

                    HttpHeaders headers = new HttpHeaders();
                    headers.setBearerAuth(testJwtFactory.generateAdminToken("admin@amerbank.com"));
                    HttpEntity<Void> entity = new HttpEntity<>(headers);

                    ResponseEntity<Void> response = restTemplate.exchange(
                            endpoint,
                            HttpMethod.DELETE,
                            entity,
                            Void.class
                    );

                    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
                    assertFalse(accountRepository.existsByAccountNumber(accountNumber));
                }

                @Test
                @DisplayName("Should return no content when account not found")
                void shouldReturnNoContentWhenAccountNotFound() {
                    Long customerId = 12L;
                    String endpoint = "/account/admin/ACCT9999999999?customerId=" + customerId;

                    HttpHeaders headers = new HttpHeaders();
                    headers.setBearerAuth(testJwtFactory.generateAdminToken("admin@amerbank.com"));
                    HttpEntity<Void> entity = new HttpEntity<>(headers);

                    ResponseEntity<String> response = restTemplate.exchange(
                            endpoint,
                            HttpMethod.DELETE,
                            entity,
                            String.class
                    );

                    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
                }
            }


            @Nested
            @DisplayName("Check Account Active")
            class CheckAccountActiveTests {

                @AfterEach
                void clearDatabase() {
                    accountRepository.deleteAllInBatch();
                }

                @Test
                @DisplayName("Should return true when account is active")
                void shouldReturnTrueWhenAccountIsActive() {
                    String accountNumber = "ACCT0000000009";
                    String endpoint = "/account/admin/" + accountNumber + "/active";

                    Account account = Account.builder()
                            .accountNumber(accountNumber)
                            .customerId(13L)
                            .balance(new BigDecimal("500.00"))
                            .type(AccountType.CHECKING)
                            .status(AccountStatus.ACTIVE)
                            .build();
                    accountRepository.save(account);

                    HttpHeaders headers = new HttpHeaders();
                    headers.setBearerAuth(testJwtFactory.generateAdminToken("admin@amerbank.com"));
                    HttpEntity<Void> entity = new HttpEntity<>(headers);

                    ResponseEntity<Boolean> response = restTemplate.exchange(
                            endpoint,
                            HttpMethod.GET,
                            entity,
                            Boolean.class
                    );

                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertTrue(response.getBody());
                }

                @Test
                @DisplayName("Should return false when account is suspended")
                void shouldReturnFalseWhenAccountIsSuspended() {
                    String accountNumber = "ACCT0000000010";
                    String endpoint = "/account/admin/" + accountNumber + "/active";

                    Account account = Account.builder()
                            .accountNumber(accountNumber)
                            .customerId(14L)
                            .balance(new BigDecimal("500.00"))
                            .type(AccountType.CHECKING)
                            .status(AccountStatus.SUSPENDED)
                            .build();
                    accountRepository.save(account);

                    HttpHeaders headers = new HttpHeaders();
                    headers.setBearerAuth(testJwtFactory.generateAdminToken("admin@amerbank.com"));
                    HttpEntity<Void> entity = new HttpEntity<>(headers);

                    ResponseEntity<Boolean> response = restTemplate.exchange(
                            endpoint,
                            HttpMethod.GET,
                            entity,
                            Boolean.class
                    );

                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertFalse(response.getBody());
                }

                @Test
                @DisplayName("Should return not found when account not found")
                void shouldReturnNotFoundWhenAccountNotFound() {
                    String endpoint = "/account/admin/ACCT9999999999/active";

                    HttpHeaders headers = new HttpHeaders();
                    headers.setBearerAuth(testJwtFactory.generateAdminToken("admin@amerbank.com"));
                    HttpEntity<Void> entity = new HttpEntity<>(headers);

                    ResponseEntity<String> response = restTemplate.exchange(
                            endpoint,
                            HttpMethod.GET,
                            entity,
                            String.class
                    );

                    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                }
            }
        }

            @Nested
            @DisplayName("Get Account Balance")
            class GetAccountBalanceTests {

                @AfterEach
                void clearDatabase() {
                    accountRepository.deleteAllInBatch();
                }

                @Test
                @DisplayName("Should get account balance by account number")
                void shouldGetAccountBalanceByAccountNumber() {
                    String accountNumber = "ACCT0000000011";
                    String endpoint = "/account/admin/" + accountNumber + "/balance";
                    BigDecimal expectedBalance = new BigDecimal("1234.56");

                    Account account = Account.builder()
                            .accountNumber(accountNumber)
                            .customerId(15L)
                            .balance(expectedBalance)
                            .type(AccountType.CHECKING)
                            .status(AccountStatus.ACTIVE)
                            .build();
                    accountRepository.save(account);

                    HttpHeaders headers = new HttpHeaders();
                    headers.setBearerAuth(testJwtFactory.generateAdminToken("admin@amerbank.com"));
                    HttpEntity<Void> entity = new HttpEntity<>(headers);

                    ResponseEntity<BigDecimal> response = restTemplate.exchange(
                            endpoint,
                            HttpMethod.GET,
                            entity,
                            BigDecimal.class
                    );

                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(expectedBalance, response.getBody());
                }

                @Test
                @DisplayName("Should return not found when account not found")
                void shouldReturnNotFoundWhenAccountNotFound() {
                    String endpoint = "/account/admin/ACCT9999999999/balance";

                    HttpHeaders headers = new HttpHeaders();
                    headers.setBearerAuth(testJwtFactory.generateAdminToken("admin@amerbank.com"));
                    HttpEntity<Void> entity = new HttpEntity<>(headers);

                    ResponseEntity<String> response = restTemplate.exchange(
                            endpoint,
                            HttpMethod.GET,
                            entity,
                            String.class
                    );

                    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                }
            }
        }
    }
