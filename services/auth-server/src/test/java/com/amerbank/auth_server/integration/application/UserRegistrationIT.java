package com.amerbank.auth_server.integration.application;

import com.amerbank.auth_server.dto.request.AdminRegisterRequest;
import com.amerbank.auth_server.dto.request.CustomerRegistrationRequest;
import com.amerbank.auth_server.dto.request.UserRegisterRequest;
import com.amerbank.auth_server.dto.response.CustomerRegistrationResponse;
import com.amerbank.auth_server.dto.response.ErrorResponse;
import com.amerbank.auth_server.dto.response.Role;
import com.amerbank.auth_server.dto.response.UserResponse;
import com.amerbank.auth_server.dto.response.ValidationErrorResponse;
import com.amerbank.auth_server.exception.CustomerRegistrationFailedException;
import com.amerbank.auth_server.model.User;
import com.amerbank.auth_server.repository.UserRepository;
import com.amerbank.auth_server.service.CustomerServiceClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.cloud.config.enabled=false",
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
@ActiveProfiles("test")
public class UserRegistrationIT {
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
    private CustomerServiceClient customerServiceClient;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void clearDatabase() {
        userRepository.deleteAllInBatch();
    }

    @Nested
    @DisplayName("User registration")
    class UserRegistration {

        @Test
        @DisplayName("Should register user successfully")
        void shouldRegisterUser() {
            String email = "test@email.com";
            String password = "testPassword";
            String endpoint = "/auth/register";
            String firstName = "Tester";
            String lastName = "Test";
            LocalDate dateOfBirth = LocalDate.of(1990, 1, 1);
            UserRegisterRequest request = new UserRegisterRequest(email, password, firstName, lastName, dateOfBirth);

            when(customerServiceClient.registerCustomer(any(CustomerRegistrationRequest.class)))
                    .thenReturn(new CustomerRegistrationResponse(100L));

            ResponseEntity<UserResponse> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    UserResponse.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            UserResponse body = response.getBody();
            assertNotNull(body.id());
            assertEquals(100L, body.customerId());
            assertEquals(email.toLowerCase(), body.email());

            Optional<User> savedUser = userRepository.findById(body.id());
            assertTrue(savedUser.isPresent());
            assertEquals(100L, savedUser.get().getCustomerId());
            assertTrue(savedUser.get().getRoles().contains(Role.ROLE_USER));

            ArgumentCaptor<CustomerRegistrationRequest> captor =
                    ArgumentCaptor.forClass(CustomerRegistrationRequest.class);
            verify(customerServiceClient).registerCustomer(captor.capture());

            CustomerRegistrationRequest outboundRequest = captor.getValue();
            assertEquals(firstName, outboundRequest.firstName());
            assertEquals(lastName, outboundRequest.lastName());
            assertEquals(dateOfBirth, outboundRequest.dateOfBirth());
            assertNotNull(outboundRequest.userId());
            assertEquals(body.id(), outboundRequest.userId());
        }

        @Test
        @DisplayName("Should throw EmailAlreadyTaken when two concurrent registrations happen")
        void shouldThrowEmailAlreadyTakenOnConcurrentRequests() throws Exception {
            String endpoint = "/auth/register";
            String email = "race@email.com";
            String password = "testPassword";
            String firstName = "Tester";
            String lastName = "Test";
            LocalDate dateOfBirth = LocalDate.of(1990, 1, 1);
            UserRegisterRequest request = new UserRegisterRequest(email, password, firstName, lastName, dateOfBirth);

            when(customerServiceClient.registerCustomer(any(CustomerRegistrationRequest.class)))
                    .thenReturn(new CustomerRegistrationResponse(100L));

            HttpEntity<UserRegisterRequest> entity = new HttpEntity<>(request);

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);

            Callable<HttpStatus> task = () -> {
                ready.countDown();
                start.await();

                ResponseEntity<String> response =
                        restTemplate.exchange(endpoint, HttpMethod.POST, entity, String.class);
                return (HttpStatus) response.getStatusCode();
            };

            Future<HttpStatus> f1 = executor.submit(task);
            Future<HttpStatus> f2 = executor.submit(task);

            ready.await();
            start.countDown();

            HttpStatus status1 = f1.get();
            HttpStatus status2 = f2.get();

            executor.shutdown();

            assertTrue(
                    (status1 == HttpStatus.CREATED && status2 == HttpStatus.CONFLICT) ||
                            (status2 == HttpStatus.CREATED && status1 == HttpStatus.CONFLICT)
            );
        }

        @Test
        @DisplayName("Should not register user when email is already taken")
        void shouldNotRegisterUserWhenEmailTaken() {
            String email = "testTaken@email.com";
            String password = "testPassword";
            String endpoint = "/auth/register";
            String firstName = "Tester";
            String lastName = "Test";
            LocalDate dateOfBirth = LocalDate.of(1990, 1, 1);
            UserRegisterRequest request = new UserRegisterRequest(email, password, firstName, lastName, dateOfBirth);

            when(customerServiceClient.registerCustomer(any(CustomerRegistrationRequest.class)))
                    .thenReturn(new CustomerRegistrationResponse(100L));

            restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    UserResponse.class
            );

            ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    ErrorResponse.class
            );

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Email already taken", response.getBody().getMessage());
            verify(customerServiceClient, times(1)).registerCustomer(any(CustomerRegistrationRequest.class));
        }

        @Test
        @DisplayName("Should roll back user when customer registration fails")
        void shouldRollbackUserWhenCustomerRegistrationFails() {
            String email = "rollback@email.com";
            String endpoint = "/auth/register";
            UserRegisterRequest request = new UserRegisterRequest(
                    email,
                    "testPassword",
                    "Tester",
                    "Rollback",
                    LocalDate.of(1990, 1, 1)
            );

            when(customerServiceClient.registerCustomer(any(CustomerRegistrationRequest.class)))
                    .thenThrow(new CustomerRegistrationFailedException("customer registration failed"));

            ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    ErrorResponse.class
            );

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Failed to register customer", response.getBody().getMessage());
            assertFalse(userRepository.existsByEmailIgnoreCase(email));
        }

        @Test
        @DisplayName("Should not register user when password has less than 4 characters")
        void shouldNotRegisterUserWhenInvalidPassword() {
            String email = "test@email.com";
            String password = "123";
            String endpoint = "/auth/register";
            String firstName = "Tester";
            String lastName = "Test";
            LocalDate dateOfBirth = LocalDate.of(1990, 1, 1);
            UserRegisterRequest request = new UserRegisterRequest(email, password, firstName, lastName, dateOfBirth);

            ResponseEntity<ValidationErrorResponse> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    ValidationErrorResponse.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Validation failed", response.getBody().getMessage());
            assertEquals("Password too short", response.getBody().getErrors().get("password"));
            assertNull(userRepository.findByEmailIgnoreCase(email).orElse(null));
        }

    }


    @Nested
    @DisplayName("Admin registration")
    class AdminTests {

        @Test
        @DisplayName("Should register admin")
        void shouldRegisterAdmin() {
            String email = "test@admin.com";
            String password = "testPassword";
            String endpoint = "/auth/admin/register";
            AdminRegisterRequest request = new AdminRegisterRequest(email, password);
            HttpEntity<AdminRegisterRequest> entity = new HttpEntity<>(request);
            ResponseEntity<UserResponse> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    UserResponse.class
            );
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            UserResponse body = response.getBody();
            assertNotNull(body.id());
            assertEquals(email.toLowerCase(), body.email());

            Optional<User> user = userRepository.findById(response.getBody().id());
            assertTrue(user.isPresent());
            assertTrue(user.get().getRoles().contains(Role.ROLE_ADMIN));
        }

        @Test
        @DisplayName("Should not register admin when email is already taken")
        void shouldNotRegisterAdminWhenEmailTaken() {
            String email = "testAdminTaken@email.com";
            String password = "testPassword";
            String endpoint = "/auth/admin/register";
            AdminRegisterRequest request = new AdminRegisterRequest(email, password);
            HttpEntity<AdminRegisterRequest> entity = new HttpEntity<>(request);

            restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    UserResponse.class
            );

            ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    ErrorResponse.class
            );
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Email already taken", response.getBody().getMessage());
        }

    }
}
