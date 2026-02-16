package com.amerbank.auth_server.integration.application;

import com.amerbank.auth_server.dto.*;
import com.amerbank.auth_server.repository.UserRepository;
import com.amerbank.auth_server.security.JwtService;
import com.amerbank.auth_server.service.CustomerServiceClient;
import com.amerbank.auth_server.util.TestJwtFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.cloud.config.enabled=false",
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
@ActiveProfiles("test")
public class UserLoginIT {

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

    @MockitoBean
    private CustomerServiceClient customerServiceClient;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @AfterEach
    void clearDatabase() {
        userRepository.deleteAllInBatch();
    }

    private UserResponse registerUser(String email, String password, String firstName, String lastName, LocalDate dateOfBirth) {
        UserRegisterRequest request = new UserRegisterRequest(email, password, firstName, lastName, dateOfBirth);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(testJwtFactory.generateServiceToken());
        HttpEntity<UserRegisterRequest> entity = new HttpEntity<>(request, headers);

        // Mock customer service to return a customer ID
        when(customerServiceClient.registerCustomer(any(CustomerRegistrationRequest.class)))
                .thenReturn(new CustomerRegistrationResponse(100L));

        ResponseEntity<UserResponse> response = restTemplate.exchange(
                "/auth/register",
                HttpMethod.POST,
                entity,
                UserResponse.class
        );
        return response.getBody();
    }

    private UserResponse registerAdmin(String email, String password) {
        AdminRegisterRequest request = new AdminRegisterRequest(email, password);
        HttpEntity<AdminRegisterRequest> entity = new HttpEntity<>(request);

        ResponseEntity<UserResponse> response = restTemplate.exchange(
                "/auth/admin/register",
                HttpMethod.POST,
                entity,
                UserResponse.class
        );
        return response.getBody();
    }

    private String login(String email, String password, String endpoint) {
        UserLoginRequest request = new UserLoginRequest(email, password);
        HttpEntity<UserLoginRequest> entity = new HttpEntity<>(request);

        ResponseEntity<?> response = restTemplate.exchange(
                endpoint,
                HttpMethod.POST,
                entity,
                Object.class
        );
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        return (String) body.get("token");
    }

    @Nested
    @DisplayName("User login")
    class UserLoginTests {

        @BeforeEach
        void setupUser() {
            registerUser("test@email.com", "password123", "John", "Doe", LocalDate.of(1990, 1, 1));
        }

        @Test
        @DisplayName("Should login user with valid credentials")
        void shouldLoginUser() {
            String token = login("test@email.com", "password123", "/auth/login");

            assertNotNull(token);
            assertFalse(jwtService.isTokenExpired(token));
        }

        @Test
        @DisplayName("Should not login user with wrong password")
        void shouldNotLoginUserWithWrongPassword() {
            UserLoginRequest request = new UserLoginRequest("test@email.com", "wrongPassword");
            HttpEntity<UserLoginRequest> entity = new HttpEntity<>(request);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/auth/login",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not login user with non-existent email")
        void shouldNotLoginUserWithNonExistentEmail() {
            UserLoginRequest request = new UserLoginRequest("nonexistent@email.com", "password123");
            HttpEntity<UserLoginRequest> entity = new HttpEntity<>(request);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/auth/login",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }

        @Test
        @DisplayName("Should normalize email case on login")
        void shouldNormalizeEmailCase() {
            String token = login("Test@Email.com", "password123", "/auth/login");

            assertNotNull(token);
            assertTrue(!jwtService.isTokenExpired(token));
        }

        @Test
        @DisplayName("Should not login user with missing password")
        void shouldNotLoginUserWithMissingPassword() {
            UserLoginRequest request = new UserLoginRequest("test@email.com", null);
            HttpEntity<UserLoginRequest> entity = new HttpEntity<>(request);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/auth/login",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not login user with missing email")
        void shouldNotLoginUserWithMissingEmail() {
            UserLoginRequest request = new UserLoginRequest(null, "password123");
            HttpEntity<UserLoginRequest> entity = new HttpEntity<>(request);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/auth/login",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Admin login")
    class AdminLoginTests {

        @BeforeEach
        void setupAdmin() {
            registerAdmin("admin@test.com", "adminPassword123");
        }

        @Test
        @DisplayName("Should login admin with valid credentials")
        void shouldLoginAdmin() {
            String token = login("admin@test.com", "adminPassword123", "/auth/admin/login");

            assertNotNull(token);
            assertTrue(!jwtService.isTokenExpired(token));
        }

        @Test
        @DisplayName("Should not allow regular user to login as admin")
        void shouldNotLoginNonAdminAsAdmin() {
            UserResponse user = registerUser("regular@user.com", "userPassword123",
                    "Jane", "Smith", LocalDate.of(1992, 5, 15));

            UserLoginRequest request = new UserLoginRequest("regular@user.com", "userPassword123");
            HttpEntity<UserLoginRequest> entity = new HttpEntity<>(request);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/auth/admin/login",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not login admin with wrong password")
        void shouldNotLoginAdminWithWrongPassword() {
            UserLoginRequest request = new UserLoginRequest("admin@test.com", "wrongPassword");
            HttpEntity<UserLoginRequest> entity = new HttpEntity<>(request);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/auth/admin/login",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not login admin with non-existent email")
        void shouldNotLoginAdminWithNonExistentEmail() {
            UserLoginRequest request = new UserLoginRequest("nonexistent@admin.com", "adminPassword123");
            HttpEntity<UserLoginRequest> entity = new HttpEntity<>(request);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/auth/admin/login",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }
    }
}