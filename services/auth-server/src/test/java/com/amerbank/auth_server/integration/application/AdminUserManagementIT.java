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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.cloud.config.enabled=false",
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
@ActiveProfiles("test")
public class AdminUserManagementIT {

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
        return (String) body.get("token");
    }

    private String loginAdmin(String email, String password) {
        UserLoginRequest request = new UserLoginRequest(email, password);
        HttpEntity<UserLoginRequest> entity = new HttpEntity<>(request);

        ResponseEntity<?> response = restTemplate.exchange(
                "/auth/admin/login",
                HttpMethod.POST,
                entity,
                Object.class
        );
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        return (String) body.get("token");
    }

    @Nested
    @DisplayName("Get all users")
    class GetAllUsers {

        @Test
        @DisplayName("Should get all users with admin token")
        void shouldGetAllUsers() {
            registerAdmin("admin@test.com", "adminPassword");
            String adminToken = loginAdmin("admin@test.com", "adminPassword");

            registerUser("user1@email.com", "password", "John", "Doe", LocalDate.of(1990, 1, 1));
            registerUser("user2@email.com", "password", "Jane", "Doe", LocalDate.of(1990, 1, 1));

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<List> response = restTemplate.exchange(
                    "/auth/admin/users",
                    HttpMethod.GET,
                    entity,
                    List.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(3, response.getBody().size()); // 1 admin + 2 users
        }



        @Test
        @DisplayName("Should not get all users with regular user token")
        void shouldNotGetAllUsersWithUserToken() {
            registerUser("user@email.com", "password", "John", "Doe", LocalDate.of(1990, 1, 1));
            String userToken = loginUser("user@email.com", "password");

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(userToken);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                    "/auth/admin/users",
                    HttpMethod.GET,
                    entity,
                    Object.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Get user by ID")
    class GetUserById {

        @Test
        @DisplayName("Should get user by ID with admin token")
        void shouldGetUserById() {
            registerAdmin("admin@test.com", "adminPassword");
            String adminToken = loginAdmin("admin@test.com", "adminPassword");

            UserResponse user = registerUser("user@email.com", "password", "John", "Doe", LocalDate.of(1990, 1, 1));

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<UserResponse> response = restTemplate.exchange(
                    "/auth/admin/users/" + user.id(),
                    HttpMethod.GET,
                    entity,
                    UserResponse.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(user.id(), response.getBody().id());
        }

        @Test
        @DisplayName("Should return 404 for non-existent user ID")
        void shouldReturn404ForNonExistentUserId() {
            registerAdmin("admin@test.com", "adminPassword");
            String adminToken = loginAdmin("admin@test.com", "adminPassword");

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<UserResponse> response = restTemplate.exchange(
                    "/auth/admin/users/99999",
                    HttpMethod.GET,
                    entity,
                    UserResponse.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Get user by email")
    class GetUserByEmail {

        @Test
        @DisplayName("Should get user by email with admin token")
        void shouldGetUserByEmail() {
            registerAdmin("admin@test.com", "adminPassword");
            String adminToken = loginAdmin("admin@test.com", "adminPassword");

            registerUser("user@email.com", "password", "John", "Doe", LocalDate.of(1990, 1, 1));

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<UserResponse> response = restTemplate.exchange(
                    "/auth/admin/users/by-email?email=user@email.com",
                    HttpMethod.GET,
                    entity,
                    UserResponse.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("user@email.com", response.getBody().email());
        }

        @Test
        @DisplayName("Should return 404 for non-existent user email")
        void shouldReturn404ForNonExistentUserEmail() {
            registerAdmin("admin@test.com", "adminPassword");
            String adminToken = loginAdmin("admin@test.com", "adminPassword");

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<UserResponse> response = restTemplate.exchange(
                    "/auth/admin/users/by-email?email=nonexistent@email.com",
                    HttpMethod.GET,
                    entity,
                    UserResponse.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Delete user")
    class DeleteUser {

        @Test
        @DisplayName("Should delete user by ID with admin token")
        void shouldDeleteUserById() {
            registerAdmin("admin@test.com", "adminPassword");
            String adminToken = loginAdmin("admin@test.com", "adminPassword");

            UserResponse user = registerUser("user@email.com", "password", "John", "Doe", LocalDate.of(1990, 1, 1));

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/auth/admin/users/" + user.id(),
                    HttpMethod.DELETE,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertFalse(userRepository.existsById(user.id()));
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent user")
        void shouldReturn404WhenDeletingNonExistentUser() {
            registerAdmin("admin@test.com", "adminPassword");
            String adminToken = loginAdmin("admin@test.com", "adminPassword");

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/auth/admin/users/99999",
                    HttpMethod.DELETE,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not delete user without admin token")
        void shouldNotDeleteUserWithoutAdminToken() {
            registerAdmin("admin@test.com", "adminPassword");
            registerUser("user@email.com", "password", "John", "Doe", LocalDate.of(1990, 1, 1));

            HttpEntity<?> entity = new HttpEntity<>(new HttpHeaders());

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/auth/admin/users/1",
                    HttpMethod.DELETE,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Update user email")
    class UpdateUserEmail {

        @Test
        @DisplayName("Should update user email with admin token")
        void shouldUpdateUserEmail() {
            registerAdmin("admin@test.com", "adminPassword");
            String adminToken = loginAdmin("admin@test.com", "adminPassword");

            UserResponse user = registerUser("user@email.com", "password", "John", "Doe", LocalDate.of(1990, 1, 1));

            AdminEmailUpdateRequest request = new AdminEmailUpdateRequest("newemail@email.com");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            HttpEntity<AdminEmailUpdateRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/auth/admin/users/" + user.id() + "/email",
                    HttpMethod.PATCH,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("Should not update user email to already taken email")
        void shouldNotUpdateUserEmailToTakenEmail() {
            registerAdmin("admin@test.com", "adminPassword");
            String adminToken = loginAdmin("admin@test.com", "adminPassword");

            registerUser("taken@email.com", "password", "Jane", "Doe", LocalDate.of(1990, 1, 1));
            UserResponse user = registerUser("user@email.com", "password", "John", "Doe", LocalDate.of(1990, 1, 1));

            AdminEmailUpdateRequest request = new AdminEmailUpdateRequest("taken@email.com");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            HttpEntity<AdminEmailUpdateRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/auth/admin/users/" + user.id() + "/email",
                    HttpMethod.PATCH,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return 404 when updating email for non-existent user")
        void shouldReturn404WhenUpdatingEmailForNonExistentUser() {
            registerAdmin("admin@test.com", "adminPassword");
            String adminToken = loginAdmin("admin@test.com", "adminPassword");

            AdminEmailUpdateRequest request = new AdminEmailUpdateRequest("newemail@email.com");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            HttpEntity<AdminEmailUpdateRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/auth/admin/users/99999/email",
                    HttpMethod.PATCH,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Update user password")
    class UpdateUserPassword {

        @Test
        @DisplayName("Should update user password with admin token")
        void shouldUpdateUserPassword() {
            registerAdmin("admin@test.com", "adminPassword");
            String adminToken = loginAdmin("admin@test.com", "adminPassword");

            UserResponse user = registerUser("user@email.com", "password", "John", "Doe", LocalDate.of(1990, 1, 1));

            AdminPasswordUpdateRequest request = new AdminPasswordUpdateRequest("newPassword123");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            HttpEntity<AdminPasswordUpdateRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/auth/admin/users/" + user.id() + "/password",
                    HttpMethod.PATCH,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("Should return 404 when updating password for non-existent user")
        void shouldReturn404WhenUpdatingPasswordForNonExistentUser() {
            registerAdmin("admin@test.com", "adminPassword");
            String adminToken = loginAdmin("admin@test.com", "adminPassword");

            AdminPasswordUpdateRequest request = new AdminPasswordUpdateRequest("newPassword123");
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            HttpEntity<AdminPasswordUpdateRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/auth/admin/users/99999/password",
                    HttpMethod.PATCH,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not update user password without admin token")
        void shouldNotUpdateUserPasswordWithoutAdminToken() {
            registerAdmin("admin@test.com", "adminPassword");
            registerUser("user@email.com", "password", "John", "Doe", LocalDate.of(1990, 1, 1));

            AdminPasswordUpdateRequest request = new AdminPasswordUpdateRequest("newPassword123");
            HttpEntity<AdminPasswordUpdateRequest> entity = new HttpEntity<>(request);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/auth/admin/users/2/password",
                    HttpMethod.PATCH,
                    entity,
                    Map.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }
}
