package com.amerbank.auth_server.integration.application;

import com.amerbank.auth_server.dto.Role;
import com.amerbank.auth_server.dto.UserRegisterRequest;
import com.amerbank.auth_server.dto.UserResponse;
import com.amerbank.auth_server.model.User;
import com.amerbank.auth_server.repository.UserRepository;
import com.amerbank.auth_server.security.JwtService;
import com.amerbank.auth_server.util.TestJwtFactory;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

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

    @TestConfiguration
    static class JwtTestConfig extends TestJwtFactory {
    }

    @Autowired
    private TestJwtFactory testJwtFactory;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;


    @Nested
    @DisplayName("User registration")
    class UserRegistration {

        @AfterEach
        void clearDatabase() {
            userRepository.deleteAllInBatch();

        }

        @Test
        @DisplayName("Should register user successfully")
        void shouldRegisterUser() {
            String email = "test@email.com";
            String password = "testPassword";
            String endpoint = "/auth/internal/register";
            String firstName = "Tester";
            String lastName = "Test";
            LocalDate dateOfBirth = LocalDate.now();
            UserRegisterRequest request = new UserRegisterRequest(email, password, firstName, lastName, dateOfBirth);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            HttpEntity<UserRegisterRequest> entity = new HttpEntity<>(request, headers);

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
        }

        @Test
        @DisplayName("Should throw EmailAlreadyTaken when two concurrent registrations happen")
        void shouldThrowEmailAlreadyTakenOnConcurrentRequests() throws Exception {
            String endpoint = "/auth/internal/register";
            String email = "race@email.com";
            String password = "testPassword";
            String firstName = "Tester";
            String lastName = "Test";
            LocalDate dateOfBirth = LocalDate.now();
            UserRegisterRequest request = new UserRegisterRequest(email, password, firstName, lastName, dateOfBirth);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());

            HttpEntity<UserRegisterRequest> entity = new HttpEntity<>(request, headers);

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);

            Callable<HttpStatus> task = () -> {
                ready.countDown();
                start.await();

                try {
                    ResponseEntity<UserResponse> response =
                            restTemplate.exchange(endpoint, HttpMethod.POST, entity, UserResponse.class);
                    return HttpStatus.valueOf(response.getStatusCode().value());
                } catch (HttpClientErrorException ex) {
                    return HttpStatus.valueOf(ex.getStatusCode().value());
                }
            };

            Future<HttpStatus> f1 = executor.submit(task);
            Future<HttpStatus> f2 = executor.submit(task);

            ready.await();
            start.countDown();

            HttpStatus status1 = f1.get();
            HttpStatus status2 = f2.get();

            executor.shutdown();

            // One must be CREATED, the other CONFLICT
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
            String endpoint = "/auth/internal/register";
            String firstName = "Tester";
            String lastName = "Test";
            LocalDate dateOfBirth = LocalDate.now();
            UserRegisterRequest request = new UserRegisterRequest(email, password, firstName, lastName, dateOfBirth);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            HttpEntity<UserRegisterRequest> entity = new HttpEntity<>(request, headers);

            restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    UserResponse.class
            );

            ResponseEntity<UserResponse> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    UserResponse.class
            );
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());
            assertNull(response.getBody().id());
            assertNull(response.getBody().email());
        }

        @Test
        @DisplayName("Should not register user when jwt provided is invalid")
        void shouldNotRegisterUserWhenInvalidJwt() {
            String email = "test@email.com";
            String password = "testPassword";
            String endpoint = "/auth/internal/register";
            String fakeToken = "FakeToken";
            String firstName = "Tester";
            String lastName = "Test";
            LocalDate dateOfBirth = LocalDate.now();
            UserRegisterRequest request = new UserRegisterRequest(email, password, firstName, lastName, dateOfBirth);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(fakeToken);
            HttpEntity<UserRegisterRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<UserResponse> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    UserResponse.class
            );
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertNotNull(response);
            assertNull(response.getBody());
        }

        @Test
        @DisplayName("Should not register user when password has less than 4 characters")
        void shouldNotRegisterUserWhenInvalidPassword() {
            String email = "test@email.com";
            String password = "123";
            String endpoint = "/auth/internal/register";
            String firstName = "Tester";
            String lastName = "Test";
            LocalDate dateOfBirth = LocalDate.now();
            UserRegisterRequest request = new UserRegisterRequest(email, password, firstName, lastName, dateOfBirth);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(testJwtFactory.generateServiceToken());
            HttpEntity<UserRegisterRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<UserResponse> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    UserResponse.class
            );
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertNull(response.getBody().id());
            assertNull(response.getBody().email());
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
            String firstName = "Tester";
            String lastName = "Test";
            LocalDate dateOfBirth = LocalDate.now();
            UserRegisterRequest request = new UserRegisterRequest(email, password, firstName, lastName, dateOfBirth);
            HttpEntity<UserRegisterRequest> entity = new HttpEntity<>(request);
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
            String firstName = "Tester";
            String lastName = "Test";
            LocalDate dateOfBirth = LocalDate.now();
            UserRegisterRequest request = new UserRegisterRequest(email, password, firstName, lastName, dateOfBirth);
            HttpEntity<UserRegisterRequest> entity = new HttpEntity<>(request);

            restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    UserResponse.class
            );

            ResponseEntity<UserResponse> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    UserResponse.class
            );
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());
            assertNull(response.getBody().id());
            assertNull(response.getBody().email());
        }

    }
}
