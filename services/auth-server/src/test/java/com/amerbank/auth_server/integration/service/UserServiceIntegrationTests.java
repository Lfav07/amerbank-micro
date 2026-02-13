package com.amerbank.auth_server.integration.service;

import com.amerbank.auth_server.dto.*;
import com.amerbank.auth_server.exception.EmailAlreadyTakenException;
import com.amerbank.auth_server.exception.UserNotFoundException;
import com.amerbank.auth_server.model.User;
import com.amerbank.auth_server.repository.UserRepository;
import com.amerbank.auth_server.service.CustomerServiceClient;
import com.amerbank.auth_server.service.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@SpringBootTest(properties = "spring.cloud.config.enabled=false")
@Testcontainers
@Transactional
@ActiveProfiles("test")
public class UserServiceIntegrationTests {

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
    private EntityManager entityManager;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("User Registration")
    class RegistrationTests {
        @Test
        @DisplayName("Should register user")
        void shouldRegisterUser() {
            String email = "myEmail@email.com";
            String normalized = "myemail@email.com";
            String password = "myPassword";
            UserRegisterRequest request = new UserRegisterRequest(
                    email, password, "John", "Doe", LocalDate.of(1990, 1, 1));

            when(customerServiceClient.registerCustomer(any(CustomerRegistrationRequest.class)))
                    .thenReturn(new CustomerRegistrationResponse(9999L));

            UserResponse response = userService.registerUser(request);

            assertNotNull(response);
            assertTrue(userRepository.existsByEmailIgnoreCase(email));
            assertEquals(normalized, response.email());
        }

        @Test
        @DisplayName("Should register admin")
        void shouldRegisterAdmin() {
            String email = "myEmail@email.com";
            String normalized = "myemail@email.com";
            String password = "myPassword";
            AdminRegisterRequest request = new AdminRegisterRequest(email, password);

            UserResponse response = userService.registerAdmin(request);
            assertNotNull(response);
            assertTrue(userRepository.existsByEmailIgnoreCase(email));
            assertEquals(normalized, response.email());
        }

        @Test
        @DisplayName("Should fail registration when email already taken")
        void shouldFailRegistrationWhenEmailTaken() {
            String email = "taken@email.com";
            String password = "myPassword";
            when(customerServiceClient.registerCustomer(any(CustomerRegistrationRequest.class)))
                    .thenReturn(new CustomerRegistrationResponse(9999L));
            UserRegisterRequest request = new UserRegisterRequest(
                    email, password, "John", "Doe", LocalDate.of(1990, 1, 1));
            userService.registerUser(request);

            UserRegisterRequest request2 = new UserRegisterRequest(
                    email, password, "Jane", "Smith", LocalDate.of(1990, 1, 1));

            assertThrows(EmailAlreadyTakenException.class, () -> userService.registerUser(request2));
        }

        @Test
        @DisplayName("Should fail admin registration when email already taken")
        void shouldFailAdminRegistrationWhenEmailTaken() {
            String email = "taken@email.com";
            String password = "myPassword";
            AdminRegisterRequest request1 = new AdminRegisterRequest(email, password);
            userService.registerAdmin(request1);

            AdminRegisterRequest request2 = new AdminRegisterRequest(email, password);

            assertThrows(EmailAlreadyTakenException.class, () -> userService.registerAdmin(request2));
        }

        @Test
        @DisplayName("Should register user with email in different case")
        void shouldRegisterUserWithDifferentCaseEmail() {
            String email = "MyEmail@EMAIL.COM";
            String normalized = "myemail@email.com";
            String password = "myPassword";
            UserRegisterRequest request = new UserRegisterRequest(
                    email, password, "John", "Doe", LocalDate.of(1990, 1, 1));

            when(customerServiceClient.registerCustomer(any(CustomerRegistrationRequest.class)))
                    .thenReturn(new CustomerRegistrationResponse(9999L));

            UserResponse response = userService.registerUser(request);

            assertNotNull(response);
            assertEquals(normalized, response.email());
            assertTrue(userRepository.existsByEmailIgnoreCase("MYEMAIL@EMAIL.COM"));
            assertTrue(userRepository.existsByEmailIgnoreCase("myemail@email.com"));
        }

        @Test
        @DisplayName("Should register user with whitespace in email")
        void shouldRegisterUserWithWhitespaceInEmail() {
            String email = "  myEmail@email.com  ";
            String normalized = "myemail@email.com";
            String password = "myPassword";
            UserRegisterRequest request = new UserRegisterRequest(
                    email, password, "John", "Doe", LocalDate.of(1990, 1, 1));

            when(customerServiceClient.registerCustomer(any(CustomerRegistrationRequest.class)))
                    .thenReturn(new CustomerRegistrationResponse(9999L));

            UserResponse response = userService.registerUser(request);

            assertNotNull(response);
            assertEquals(normalized, response.email());
        }
    }

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("User log-in")
    class LoginTests {

        @Test
        @DisplayName("Should log-in user")
        void shouldLoginUser() {
            String email = "myemail@email.com";
            String password = "myPassword";
            UserRegisterRequest registerRequest = new UserRegisterRequest(
                    email, password, "John", "Doe", LocalDate.of(1990, 1, 1));

            when(customerServiceClient.registerCustomer(any(CustomerRegistrationRequest.class)))
                    .thenReturn(new CustomerRegistrationResponse(100L));

            userService.registerUser(registerRequest);
            UserLoginRequest request = new UserLoginRequest(email, password);

            AuthenticationResponse response = userService.login(request);
            assertNotNull(response.token());
        }

        @Test
        @DisplayName("Should log-in admin")
        void shouldLoginAdmin() {
            String email = "myemail@email.com";
            String password = "myPassword";
            AdminRegisterRequest registerRequest = new AdminRegisterRequest(email, password);
            userService.registerAdmin(registerRequest);
            UserLoginRequest request = new UserLoginRequest(email, password);

            AuthenticationResponse response = userService.loginAdmin(request);
            assertNotNull(response.token());
        }

        @Test
        @DisplayName("Should fail login with wrong password")
        void shouldFailLoginWithWrongPassword() {
            String email = "myemail@email.com";
            String password = "myPassword";
            UserRegisterRequest registerRequest = new UserRegisterRequest(
                    email, password, "John", "Doe", LocalDate.of(1990, 1, 1));

            when(customerServiceClient.registerCustomer(any(CustomerRegistrationRequest.class)))
                    .thenReturn(new CustomerRegistrationResponse(100L));

            userService.registerUser(registerRequest);
            UserLoginRequest request = new UserLoginRequest(email, "wrongPassword");

            assertThrows(Exception.class, () -> userService.login(request));
        }

        @Test
        @DisplayName("Should fail login with non-existent email")
        void shouldFailLoginWithNonExistentEmail() {
            String email = "nonexistent@email.com";
            String password = "myPassword";
            UserLoginRequest request = new UserLoginRequest(email, password);

            assertThrows(BadCredentialsException.class, () -> userService.login(request));
        }

        @Test
        @DisplayName("Should fail user login via admin endpoint")
        void shouldFailUserLoginViaAdminEndpoint() {
            String email = "myemail@email.com";
            String password = "myPassword";
            UserRegisterRequest registerRequest = new UserRegisterRequest(
                    email, password, "John", "Doe", LocalDate.of(1990, 1, 1));

            when(customerServiceClient.registerCustomer(any(CustomerRegistrationRequest.class)))
                    .thenReturn(new CustomerRegistrationResponse(100L));

            userService.registerUser(registerRequest);
            UserLoginRequest request = new UserLoginRequest(email, password);

            assertThrows(Exception.class, () -> userService.loginAdmin(request));
        }

        @Test
        @DisplayName("Should login with email in different case")
        void shouldLoginWithDifferentCaseEmail() {
            String email = "MyEmail@EMAIL.COM";
            String password = "myPassword";
            UserRegisterRequest registerRequest = new UserRegisterRequest(
                    email, password, "John", "Doe", LocalDate.of(1990, 1, 1));

            when(customerServiceClient.registerCustomer(any(CustomerRegistrationRequest.class)))
                    .thenReturn(new CustomerRegistrationResponse(100L));

            userService.registerUser(registerRequest);
            UserLoginRequest request = new UserLoginRequest("MYEMAIL@EMAIL.COM", password);

            AuthenticationResponse response = userService.login(request);
            assertNotNull(response.token());
        }
    }

    // -------------------------------------------------------------------------
    // Update Email
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Update Email")
    class UpdateEmailTests {

        @Test
        @DisplayName("Should update user email")
        void shouldUpdateUserEmail() {
            String email = "old@email.com";
            String password = "myPassword";
            UserRegisterRequest registerRequest = new UserRegisterRequest(
                    email, password, "John", "Doe", LocalDate.of(1990, 1, 1));

            when(customerServiceClient.registerCustomer(any(CustomerRegistrationRequest.class)))
                    .thenReturn(new CustomerRegistrationResponse(100L));

            UserResponse registered = userService.registerUser(registerRequest);

            String newEmail = "new@email.com";
            userService.updateEmail(registered.id(), newEmail);

            User updated = userRepository.findById(registered.id()).orElseThrow();
            assertEquals("new@email.com", updated.getEmail());
        }

        @Test
        @DisplayName("Should update user email by admin")
        void shouldUpdateUserEmailByAdmin() {
            String email = "old@email.com";
            String password = "myPassword";
            UserRegisterRequest registerRequest = new UserRegisterRequest(
                    email, password, "John", "Doe", LocalDate.of(1990, 1, 1));

            when(customerServiceClient.registerCustomer(any(CustomerRegistrationRequest.class)))
                    .thenReturn(new CustomerRegistrationResponse(100L));

            UserResponse registered = userService.registerUser(registerRequest);

            String newEmail = "new@email.com";
            userService.updateEmailById(1L, registered.id(), newEmail);

            User updated = userRepository.findById(registered.id()).orElseThrow();
            assertEquals("new@email.com", updated.getEmail());
        }

        @Test
        @DisplayName("Should fail update email when new email is taken")
        void shouldFailUpdateEmailWhenTaken() {
            String email1 = "user1@email.com";
            String email2 = "user2@email.com";
            String password = "myPassword";

            when(customerServiceClient.registerCustomer(any(CustomerRegistrationRequest.class)))
                    .thenReturn(new CustomerRegistrationResponse(100L))
                    .thenReturn(new CustomerRegistrationResponse(101L));

            UserRegisterRequest request1 = new UserRegisterRequest(
                    email1, password, "John", "Doe", LocalDate.of(1990, 1, 1));
            UserRegisterRequest request2 = new UserRegisterRequest(
                    email2, password, "Jane", "Smith", LocalDate.of(1990, 1, 1));
            UserResponse registered1 = userService.registerUser(request1);
            userService.registerUser(request2);

            assertThrows(EmailAlreadyTakenException.class,
                    () -> userService.updateEmail(registered1.id(), email2));
        }

        @Test
        @DisplayName("Should fail update email for non-existent user")
        void shouldFailUpdateEmailForNonExistentUser() {
            assertThrows(UserNotFoundException.class,
                    () -> userService.updateEmail(999L, "new@email.com"));
        }

        @Test
        @DisplayName("Should update email with whitespace and normalize")
        void shouldUpdateEmailWithWhitespace() {
            String email = "old@email.com";
            String password = "myPassword";
            UserRegisterRequest registerRequest = new UserRegisterRequest(
                    email, password, "John", "Doe", LocalDate.of(1990, 1, 1));

            when(customerServiceClient.registerCustomer(any(CustomerRegistrationRequest.class)))
                    .thenReturn(new CustomerRegistrationResponse(100L));

            UserResponse registered = userService.registerUser(registerRequest);

            String newEmail = "  NEW@EMAIL.COM  ";
            userService.updateEmail(registered.id(), newEmail);

            User updated = userRepository.findById(registered.id()).orElseThrow();
            assertEquals("new@email.com", updated.getEmail());
        }
    }

    // -------------------------------------------------------------------------
    // Update Password
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Update Password")
    class UpdatePasswordTests {

        @Test
        @DisplayName("Should update user password")
        void shouldUpdateUserPassword() {
            String email = "myemail@email.com";
            String oldPassword = "oldPassword";
            String newPassword = "newPassword";
            UserRegisterRequest registerRequest = new UserRegisterRequest(
                    email, oldPassword, "John", "Doe", LocalDate.of(1990, 1, 1));

            when(customerServiceClient.registerCustomer(any(CustomerRegistrationRequest.class)))
                    .thenReturn(new CustomerRegistrationResponse(100L));

            UserResponse registered = userService.registerUser(registerRequest);

            PasswordUpdateRequest updateRequest = new PasswordUpdateRequest(oldPassword, newPassword);
            userService.updatePassword(registered.id(), updateRequest);

            User updated = userRepository.findById(registered.id()).orElseThrow();
            assertNotEquals(oldPassword, updated.getPassword());
        }

        @Test
        @DisplayName("Should update user password by admin")
        void shouldUpdateUserPasswordByAdmin() {
            String email = "myemail@email.com";
            String password = "myPassword";
            UserRegisterRequest registerRequest = new UserRegisterRequest(
                    email, password, "John", "Doe", LocalDate.of(1990, 1, 1));

            when(customerServiceClient.registerCustomer(any(CustomerRegistrationRequest.class)))
                    .thenReturn(new CustomerRegistrationResponse(100L));

            UserResponse registered = userService.registerUser(registerRequest);

            String newPassword = "newPassword";
            userService.updatePasswordById(1L, registered.id(), newPassword);

            User updated = userRepository.findById(registered.id()).orElseThrow();
            assertNotEquals(password, updated.getPassword());
        }

        @Test
        @DisplayName("Should fail update password with wrong current password")
        void shouldFailUpdatePasswordWithWrongCurrentPassword() {
            String email = "myemail@email.com";
            String oldPassword = "oldPassword";
            String newPassword = "newPassword";
            UserRegisterRequest registerRequest = new UserRegisterRequest(
                    email, oldPassword, "John", "Doe", LocalDate.of(1990, 1, 1));

            when(customerServiceClient.registerCustomer(any(CustomerRegistrationRequest.class)))
                    .thenReturn(new CustomerRegistrationResponse(100L));

            UserResponse registered = userService.registerUser(registerRequest);

            PasswordUpdateRequest updateRequest = new PasswordUpdateRequest("wrongPassword", newPassword);

            assertThrows(Exception.class, () -> userService.updatePassword(registered.id(), updateRequest));
        }

        @Test
        @DisplayName("Should fail update password for non-existent user")
        void shouldFailUpdatePasswordForNonExistentUser() {
            PasswordUpdateRequest updateRequest = new PasswordUpdateRequest("oldPassword", "newPassword");

            assertThrows(UserNotFoundException.class,
                    () -> userService.updatePassword(999L, updateRequest));
        }
    }

    // -------------------------------------------------------------------------
    // Retrieval
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Retrieval")
    class RetrievalTests {

        @Test
        @DisplayName("Should find user by email")
        void shouldFindUserByEmail() {
            String email = "myemail@email.com";
            String password = "myPassword";
            UserRegisterRequest registerRequest = new UserRegisterRequest(
                    email, password, "John", "Doe", LocalDate.of(1990, 1, 1));

            when(customerServiceClient.registerCustomer(any(CustomerRegistrationRequest.class)))
                    .thenReturn(new CustomerRegistrationResponse(100L));

            userService.registerUser(registerRequest);

            User found = userService.findByEmail("MYEMAIL@EMAIL.COM");

            assertNotNull(found);
            assertEquals("myemail@email.com", found.getEmail());
        }

        @Test
        @DisplayName("Should find user by email mapped to response")
        void shouldFindUserByEmailMapped() {
            String email = "myemail@email.com";
            String password = "myPassword";
            UserRegisterRequest registerRequest = new UserRegisterRequest(
                    email, password, "John", "Doe", LocalDate.of(1990, 1, 1));

            when(customerServiceClient.registerCustomer(any(CustomerRegistrationRequest.class)))
                    .thenReturn(new CustomerRegistrationResponse(100L));

            userService.registerUser(registerRequest);

            UserResponse found = userService.findByEmailMapped("myemail@email.com");

            assertNotNull(found);
            assertEquals("myemail@email.com", found.email());
        }

        @Test
        @DisplayName("Should find user by ID")
        void shouldFindUserById() {
            String email = "myemail@email.com";
            String password = "myPassword";
            UserRegisterRequest registerRequest = new UserRegisterRequest(
                    email, password, "John", "Doe", LocalDate.of(1990, 1, 1));

            when(customerServiceClient.registerCustomer(any(CustomerRegistrationRequest.class)))
                    .thenReturn(new CustomerRegistrationResponse(100L));

            UserResponse registered = userService.registerUser(registerRequest);

            User found = userService.findById(registered.id());

            assertNotNull(found);
            assertEquals(email.toLowerCase(), found.getEmail());
        }

        @Test
        @DisplayName("Should find user by ID mapped to response")
        void shouldFindUserByIdMapped() {
            String email = "myemail@email.com";
            String password = "myPassword";
            UserRegisterRequest registerRequest = new UserRegisterRequest(
                    email, password, "John", "Doe", LocalDate.of(1990, 1, 1));

            when(customerServiceClient.registerCustomer(any(CustomerRegistrationRequest.class)))
                    .thenReturn(new CustomerRegistrationResponse(100L));

            UserResponse registered = userService.registerUser(registerRequest);

            UserResponse found = userService.findByIdMapped(registered.id());

            assertNotNull(found);
            assertEquals(email.toLowerCase(), found.email());
        }

        @Test
        @DisplayName("Should get own user info")
        void shouldGetOwnUserInfo() {
            String email = "myemail@email.com";
            String password = "myPassword";
            UserRegisterRequest registerRequest = new UserRegisterRequest(
                    email, password, "John", "Doe", LocalDate.of(1990, 1, 1));

            when(customerServiceClient.registerCustomer(any(CustomerRegistrationRequest.class)))
                    .thenReturn(new CustomerRegistrationResponse(100L));

            UserResponse registered = userService.registerUser(registerRequest);

            UserResponse found = userService.getOwnUserInfo(registered.id());

            assertNotNull(found);
            assertEquals(email.toLowerCase(), found.email());
        }

        @Test
        @DisplayName("Should get all users")
        void shouldGetAllUsers() {
            String email1 = "user1@email.com";
            String email2 = "user2@email.com";
            String password = "myPassword";

            when(customerServiceClient.registerCustomer(any(CustomerRegistrationRequest.class)))
                    .thenReturn(new CustomerRegistrationResponse(100L))
                    .thenReturn(new CustomerRegistrationResponse(101L));

            userService.registerUser(new UserRegisterRequest(
                    email1, password, "John", "Doe", LocalDate.of(1990, 1, 1)));
            userService.registerUser(new UserRegisterRequest(
                    email2, password, "Jane", "Smith", LocalDate.of(1990, 1, 1)));

            List<UserResponse> users = userService.getAllUsers();

            assertEquals(2, users.size());
        }

        @Test
        @DisplayName("Should fail find user by non-existent email")
        void shouldFailFindByNonExistentEmail() {
            assertThrows(UserNotFoundException.class,
                    () -> userService.findByEmail("nonexistent@email.com"));
        }

        @Test
        @DisplayName("Should fail find user by non-existent ID")
        void shouldFailFindByNonExistentId() {
            assertThrows(UserNotFoundException.class,
                    () -> userService.findById(999L));
        }

        @Test
        @DisplayName("Should return false for email not taken")
        void shouldReturnFalseForEmailNotTaken() {
            boolean taken = userService.isEmailTaken("nottaken@email.com");

            assertFalse(taken);
        }

        @Test
        @DisplayName("Should return true for email taken")
        void shouldReturnTrueForEmailTaken() {
            String email = "myemail@email.com";
            String password = "myPassword";

            when(customerServiceClient.registerCustomer(any(CustomerRegistrationRequest.class)))
                    .thenReturn(new CustomerRegistrationResponse(100L));

            userService.registerUser(new UserRegisterRequest(
                    email, password, "John", "Doe", LocalDate.of(1990, 1, 1)));

            boolean taken = userService.isEmailTaken("MYEMAIL@EMAIL.COM");

            assertTrue(taken);
        }
    }

    // -------------------------------------------------------------------------
    // Deletion
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Deletion")
    class DeletionTests {

        @Test
        @DisplayName("Should delete user by admin")
        void shouldDeleteUserByAdmin() {
            String email = "myemail@email.com";
            String password = "myPassword";
            UserRegisterRequest registerRequest = new UserRegisterRequest(
                    email, password, "John", "Doe", LocalDate.of(1990, 1, 1));

            when(customerServiceClient.registerCustomer(any(CustomerRegistrationRequest.class)))
                    .thenReturn(new CustomerRegistrationResponse(100L));

            UserResponse registered = userService.registerUser(registerRequest);

            userService.deleteUser(1L, registered.id());

            assertFalse(userRepository.existsById(registered.id()));
        }

        @Test
        @DisplayName("Should delete all users")
        void shouldDeleteAllUsers() {
            String email1 = "user1@email.com";
            String email2 = "user2@email.com";
            String password = "myPassword";

            when(customerServiceClient.registerCustomer(any(CustomerRegistrationRequest.class)))
                    .thenReturn(new CustomerRegistrationResponse(100L))
                    .thenReturn(new CustomerRegistrationResponse(101L));

            userService.registerUser(new UserRegisterRequest(
                    email1, password, "John", "Doe", LocalDate.of(1990, 1, 1)));
            userService.registerUser(new UserRegisterRequest(
                    email2, password, "Jane", "Smith", LocalDate.of(1990, 1, 1)));

            userService.deleteAllUsers();

            assertEquals(0, userRepository.count());
        }

        @Test
        @DisplayName("Should handle delete non-existent user gracefully")
        void shouldHandleDeleteNonExistentUser() {
            assertDoesNotThrow(() -> userService.deleteUser(1L, 999L));
        }
    }
}