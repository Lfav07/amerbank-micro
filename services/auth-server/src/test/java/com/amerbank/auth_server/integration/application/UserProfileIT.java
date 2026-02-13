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
import org.springframework.http.*;
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
public class UserProfileIT {

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

    private long customerIdCounter = 100L;

    @AfterEach
    void clearDatabase() {
        userRepository.deleteAllInBatch();
    }

    private UserResponse registerUser(String email, String password, String firstName, String lastName, LocalDate dateOfBirth) {
        UserRegisterRequest request = new UserRegisterRequest(email, password, firstName, lastName, dateOfBirth);
        HttpEntity<UserRegisterRequest> entity = new HttpEntity<>(request);

        long customerId = customerIdCounter++;
        when(customerServiceClient.registerCustomer(any(CustomerRegistrationRequest.class)))
                .thenReturn(new CustomerRegistrationResponse(customerId));

        ResponseEntity<UserResponse> response = restTemplate.exchange(
                "/auth/register",
                HttpMethod.POST,
                entity,
                UserResponse.class
        );
        return response.getBody();
    }

    private String loginUser(String email, String password) {
        UserLoginRequest request = new UserLoginRequest(email, password);
        HttpEntity<UserLoginRequest> entity = new HttpEntity<>(request);

        ResponseEntity<?> response = restTemplate.exchange(
                "/auth/login",
                HttpMethod.POST,
                entity,
                Object.class
        );
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        return (String) body.get("token");
    }

    @Nested
    @DisplayName("Get own user info")
    class GetOwnUserInfo {

        private String userToken;
        private UserResponse registeredUser;

        @BeforeEach
        void setup() {
            registeredUser = registerUser("test@email.com", "password123", "John", "Doe", LocalDate.of(1990, 1, 1));
            userToken = loginUser("test@email.com", "password123");
        }

        @Test
        @DisplayName("Should get own user info with valid token")
        void shouldGetOwnUserInfo() {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(userToken);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<UserResponse> response = restTemplate.exchange(
                    "/auth/me",
                    HttpMethod.GET,
                    entity,
                    UserResponse.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(registeredUser.id(), response.getBody().id());
            assertEquals("test@email.com", response.getBody().email());
        }

        @Test
        @DisplayName("Should not get user info without token")
        void shouldNotGetOwnUserInfoWithoutToken() {
            HttpEntity<?> entity = new HttpEntity<>(new HttpHeaders());

            ResponseEntity<UserResponse> response = restTemplate.exchange(
                    "/auth/me",
                    HttpMethod.GET,
                    entity,
                    UserResponse.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not get user info with invalid token")
        void shouldNotGetOwnUserInfoWithInvalidToken() {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth("invalidToken");
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<UserResponse> response = restTemplate.exchange(
                    "/auth/me",
                    HttpMethod.GET,
                    entity,
                    UserResponse.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Update email")
    class UpdateEmail {

        private String userToken;

        @BeforeEach
        void setup() {
            registerUser("test@email.com", "password123", "John", "Doe", LocalDate.of(1990, 1, 1));
            userToken = loginUser("test@email.com", "password123");
        }

        @Test
        @DisplayName("Should update own email with valid token")
        void shouldUpdateOwnEmail() {
            EmailUpdateRequest request = new EmailUpdateRequest("newemail@email.com", "password123");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(userToken);
            HttpEntity<EmailUpdateRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/auth/me/email",
                    HttpMethod.PATCH,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Email successfully updated", response.getBody().get("message"));
        }

        @Test
        @DisplayName("Should not update email to already taken email")
        void shouldNotUpdateEmailToTakenEmail() {
            registerUser("taken@email.com", "password123", "Jane", "Doe", LocalDate.of(1990, 1, 1));
            EmailUpdateRequest request = new EmailUpdateRequest("taken@email.com", "password123");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(userToken);
            HttpEntity<EmailUpdateRequest> entity = new HttpEntity<>(request, headers);


            ResponseEntity<Map> response = restTemplate.exchange(
                    "/auth/me/email",
                    HttpMethod.PATCH,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not update email without token")
        void shouldNotUpdateEmailWithoutToken() {
            EmailUpdateRequest request = new EmailUpdateRequest("newemail@email.com", "password123");
            HttpEntity<EmailUpdateRequest> entity = new HttpEntity<>(request);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/auth/me/email",
                    HttpMethod.PATCH,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }

        @Test
        @DisplayName("Should update email with whitespace and normalize")
        void shouldUpdateEmailWithWhitespace() {
            EmailUpdateRequest request = new EmailUpdateRequest("  NEW@EMAIL.COM  ", "password123");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(userToken);
            HttpEntity<EmailUpdateRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/auth/me/email",
                    HttpMethod.PATCH,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Update password")
    class UpdatePassword {

        private String userToken;

        @BeforeEach
        void setup() {
            registerUser("test@email.com", "password123", "John", "Doe", LocalDate.of(1990, 1, 1));
            userToken = loginUser("test@email.com", "password123");
        }

        @Test
        @DisplayName("Should update own password with valid token")
        void shouldUpdateOwnPassword() {
            PasswordUpdateRequest request = new PasswordUpdateRequest("password123", "newPassword456");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(userToken);
            HttpEntity<PasswordUpdateRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/auth/me/password",
                    HttpMethod.PATCH,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Password successfully updated", response.getBody().get("message"));
        }

        @Test
        @DisplayName("Should not update password with wrong current password")
        void shouldNotUpdatePasswordWithWrongCurrentPassword() {
            PasswordUpdateRequest request = new PasswordUpdateRequest("wrongPassword", "newPassword456");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(userToken);
            HttpEntity<PasswordUpdateRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/auth/me/password",
                    HttpMethod.PATCH,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not update password without token")
        void shouldNotUpdatePasswordWithoutToken() {
            PasswordUpdateRequest request = new PasswordUpdateRequest("password123", "newPassword456");
            HttpEntity<PasswordUpdateRequest> entity = new HttpEntity<>(request);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/auth/me/password",
                    HttpMethod.PATCH,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }
}
