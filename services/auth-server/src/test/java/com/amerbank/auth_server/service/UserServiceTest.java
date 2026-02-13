package com.amerbank.auth_server.service;

import com.amerbank.auth_server.dto.*;
import com.amerbank.auth_server.model.User;
import com.amerbank.auth_server.repository.UserRepository;
import com.amerbank.auth_server.security.JwtService;
import com.amerbank.auth_server.exception.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private CustomerServiceClient customerServiceClient;
    @Mock
    private JwtService jwtService;

    @Spy
    private final UserMapper mapper = new UserMapper();

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Should return true when email already exists")
    void shouldReturnTrueWhenEmailExists() {
        String email = "Tester@Email.com";
        String normalized = "tester@email.com";

        when(userRepository.existsByEmailIgnoreCase(normalized))
                .thenReturn(true);

        boolean result = userService.isEmailTaken(email);

        assertTrue(result);
        verify(userRepository).existsByEmailIgnoreCase(normalized);
    }

    @Test
    @DisplayName("Should return false when email is not taken")
    void shouldReturnFalseWhenEmailNotTaken() {
        String email = "available@email.com";
        String normalized = "available@email.com";

        when(userRepository.existsByEmailIgnoreCase(normalized)).thenReturn(false);

        boolean result = userService.isEmailTaken(email);

        assertFalse(result);
        verify(userRepository).existsByEmailIgnoreCase(normalized);
    }

    @Test
    @DisplayName("Should normalize email with whitespace when checking if email is taken")
    void shouldNormalizeEmailWithWhitespaceWhenCheckingIfTaken() {
        String email = "  TEST@EMAIL.COM  ";
        String normalized = "test@email.com";

        when(userRepository.existsByEmailIgnoreCase(normalized)).thenReturn(true);

        boolean result = userService.isEmailTaken(email);

        assertTrue(result);
        verify(userRepository).existsByEmailIgnoreCase(normalized);
    }

    @Test
    @DisplayName("Should successfully register new user with customer creation")
    void shouldRegisterNewUser() {
        String email = "Tester@email.com";
        String normalizedEmail = "tester@email.com";
        String password = "myPassword";
        String encodedPassword = "encoded";
        String firstName = "John";
        String lastName = "Doe";
        LocalDate dateOfBirth = LocalDate.of(1990, 1, 1);
        Long customerId = 100L;

        UserRegisterRequest request = new UserRegisterRequest(
                email, password, firstName, lastName, dateOfBirth);

        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(userRepository.existsByEmailIgnoreCase(normalizedEmail)).thenReturn(false);

        User savedUser = User.builder()
                .id(1L)
                .email(normalizedEmail)
                .password(encodedPassword)
                .active(true)
                .roles(Set.of(Role.ROLE_USER))
                .build();

        when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedUser);

        CustomerRegistrationResponse customerResponse =
                new CustomerRegistrationResponse(customerId);
        when(customerServiceClient.registerCustomer(any(CustomerRegistrationRequest.class)))
                .thenReturn(customerResponse);

        User updatedUser = User.builder()
                .id(1L)
                .email(normalizedEmail)
                .password(encodedPassword)
                .active(true)
                .customerId(customerId)
                .roles(Set.of(Role.ROLE_USER))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(updatedUser));

        UserResponse response = userService.registerUser(request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(normalizedEmail, response.email());

        verify(userRepository).saveAndFlush(argThat(user ->
                user.getEmail().equals(normalizedEmail) &&
                        user.getPassword().equals(encodedPassword) &&
                        user.isActive() &&
                        user.getRoles().contains(Role.ROLE_USER)
        ));

        verify(customerServiceClient).registerCustomer(argThat(req ->
                req.firstName().equals(firstName) &&
                        req.lastName().equals(lastName) &&
                        req.userId().equals(1L) &&
                        req.dateOfBirth().equals(dateOfBirth)
        ));
    }

    @Test
    @DisplayName("Should rollback user creation when customer service is unavailable")
    void shouldRollbackUserCreationWhenCustomerServiceUnavailable() {
        String email = "test@email.com";
        UserRegisterRequest request = new UserRegisterRequest(
                email, "password", "John", "Doe", LocalDate.of(1990, 1, 1));

        User savedUser = User.builder()
                .id(1L)
                .email("test@email.com")
                .password("encoded")
                .active(true)
                .roles(Set.of(Role.ROLE_USER))
                .build();

        when(userRepository.existsByEmailIgnoreCase("test@email.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedUser);
        when(customerServiceClient.registerCustomer(any()))
                .thenThrow(new CustomerServiceUnavailableException("Service down"));

        assertThrows(CustomerServiceUnavailableException.class,
                () -> userService.registerUser(request));

        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should rollback user creation when customer registration fails")
    void shouldRollbackUserCreationWhenCustomerRegistrationFails() {
        String email = "test@email.com";
        UserRegisterRequest request = new UserRegisterRequest(
                email, "password", "John", "Doe", LocalDate.of(1990, 1, 1));

        User savedUser = User.builder()
                .id(1L)
                .email("test@email.com")
                .password("encoded")
                .active(true)
                .roles(Set.of(Role.ROLE_USER))
                .build();

        when(userRepository.existsByEmailIgnoreCase("test@email.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedUser);
        when(customerServiceClient.registerCustomer(any()))
                .thenThrow(new CustomerRegistrationFailedException("Registration failed"));

        assertThrows(CustomerRegistrationFailedException.class,
                () -> userService.registerUser(request));

        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should rollback user creation and throw RegistrationFailedException on unexpected error")
    void shouldRollbackUserCreationOnUnexpectedError() {
        String email = "test@email.com";
        UserRegisterRequest request = new UserRegisterRequest(
                email, "password", "John", "Doe", LocalDate.of(1990, 1, 1));

        User savedUser = User.builder()
                .id(1L)
                .email("test@email.com")
                .password("encoded")
                .active(true)
                .roles(Set.of(Role.ROLE_USER))
                .build();

        when(userRepository.existsByEmailIgnoreCase("test@email.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedUser);
        when(customerServiceClient.registerCustomer(any()))
                .thenThrow(new RuntimeException("Unexpected error"));

        assertThrows(RegistrationFailedException.class,
                () -> userService.registerUser(request));

        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw EmailAlreadyTakenException when registering with existing email")
    void shouldThrowExceptionWhenRegisteringWithTakenEmail() {
        String email = "existing@email.com";
        String normalized = "existing@email.com";
        UserRegisterRequest request = new UserRegisterRequest(
                email, "password", "John", "Doe", LocalDate.of(1990, 1, 1));

        when(userRepository.existsByEmailIgnoreCase(normalized)).thenReturn(true);

        assertThrows(EmailAlreadyTakenException.class,
                () -> userService.registerUser(request));

        verify(userRepository, never()).saveAndFlush(any());
        verify(customerServiceClient, never()).registerCustomer(any());
    }

    @Test
    @DisplayName("Should throw EmailAlreadyTakenException when DataIntegrityViolationException occurs during user creation")
    void shouldThrowEmailAlreadyTakenExceptionWhenDataIntegrityViolationDuringUserCreation() {
        String email = "test@email.com";
        UserRegisterRequest request = new UserRegisterRequest(
                email, "password", "John", "Doe", LocalDate.of(1990, 1, 1));

        when(userRepository.existsByEmailIgnoreCase(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        assertThrows(EmailAlreadyTakenException.class,
                () -> userService.registerUser(request));
    }

    @Test
    @DisplayName("Should normalize email to lowercase when registering user")
    void shouldNormalizeEmailWhenRegisteringUser() {
        String email = "TEST@EMAIL.COM";
        String normalized = "test@email.com";
        UserRegisterRequest request = new UserRegisterRequest(
                email, "password", "John", "Doe", LocalDate.of(1990, 1, 1));

        User savedUser = User.builder()
                .id(1L)
                .email(normalized)
                .password("encoded")
                .active(true)
                .roles(Set.of(Role.ROLE_USER))
                .build();

        when(userRepository.existsByEmailIgnoreCase(normalized)).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedUser);
        when(customerServiceClient.registerCustomer(any()))
                .thenReturn(new CustomerRegistrationResponse(100L));
        when(userRepository.findById(1L)).thenReturn(Optional.of(savedUser));

        userService.registerUser(request);

        verify(userRepository).saveAndFlush(argThat(user ->
                user.getEmail().equals(normalized)
        ));
    }

    @Test
    @DisplayName("Should successfully register new admin")
    void shouldRegisterAdmin() {
        AdminRegisterRequest request =
                new AdminRegisterRequest("ADMIN@EMAIL.COM", "password");

        when(userRepository.existsByEmailIgnoreCase("admin@email.com"))
                .thenReturn(false);

        when(passwordEncoder.encode(any()))
                .thenReturn("encoded");

        userService.registerAdmin(request);

        verify(userRepository).save(argThat(user ->
                user.getEmail().equals("admin@email.com") &&
                        user.getPassword().equals("encoded") &&
                        user.isActive() &&
                        user.getRoles().contains(Role.ROLE_ADMIN)
        ));
    }

    @Test
    @DisplayName("Should throw EmailAlreadyTakenException when registering admin with existing email")
    void shouldThrowExceptionWhenRegisteringAdminWithTakenEmail() {
        String email = "existing@email.com";
        String normalized = "existing@email.com";
        AdminRegisterRequest request = new AdminRegisterRequest(email, "password");

        when(userRepository.existsByEmailIgnoreCase(normalized)).thenReturn(true);

        assertThrows(EmailAlreadyTakenException.class,
                () -> userService.registerAdmin(request));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw EmailAlreadyTakenException when DataIntegrityViolationException occurs during admin registration")
    void shouldThrowEmailAlreadyTakenExceptionWhenDataIntegrityViolationDuringAdminRegistration() {
        String email = "admin@email.com";
        AdminRegisterRequest request = new AdminRegisterRequest(email, "password");

        when(userRepository.existsByEmailIgnoreCase(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        assertThrows(EmailAlreadyTakenException.class,
                () -> userService.registerAdmin(request));
    }

    @Test
    @DisplayName("Should successfully log-in user")
    void shouldLoginUser() {
        String email = "Tester@email.com";
        String normalized = "tester@email.com";
        String password = "myPassword";
        UserLoginRequest request = new UserLoginRequest(email, password);
        Long customerId = 1L;
        String jwtToken = "TestToken";

        User user = User.builder()
                .id(1L)
                .email(normalized)
                .password(password)
                .active(true)
                .customerId(customerId)
                .roles(Set.of(Role.ROLE_USER))
                .build();

        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(userRepository.findByEmailIgnoreCase(normalized)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user, customerId)).thenReturn(jwtToken);

        AuthenticationResponse response = userService.login(request);

        assertEquals(jwtToken, response.token());
        verify(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService)
                .generateToken(user, customerId);
    }

    @Test
    @DisplayName("Should throw exception when password authentication fails during login")
    void shouldThrowExceptionWhenPasswordAuthenticationFailsDuringLogin() {
        String email = "test@email.com";
        String password = "wrongPassword";
        UserLoginRequest request = new UserLoginRequest(email, password);

        when(authenticationManager.authenticate(any()))
                .thenThrow(new RuntimeException("Bad credentials"));

        assertThrows(RuntimeException.class,
                () -> userService.login(request));

        verify(jwtService, never()).generateToken(any(), any());
    }

    @Test
    @DisplayName("Should successfully log-in admin")
    void shouldLoginAdmin() {
        String email = "tester@email.com";
        String password = "myPassword";
        UserLoginRequest request = new UserLoginRequest(email, password);
        String jwtToken = "TestAdminToken";

        User user = User.builder()
                .id(1L)
                .email(email)
                .password(password)
                .active(true)
                .roles(Set.of(Role.ROLE_ADMIN))
                .build();

        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(jwtService.generateAdminToken(user)).thenReturn(jwtToken);

        AuthenticationResponse response = userService.loginAdmin(request);

        assertEquals(jwtToken, response.token());
        verify(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService)
                .generateAdminToken(user);
    }

    @Test
    @DisplayName("Should deny admin login when user is not admin")
    void shouldDenyAdminLoginForNonAdminUser() {
        String email = "user@email.com";
        String password = "password";
        UserLoginRequest request = new UserLoginRequest(email, password);

        User user = User.builder()
                .id(1L)
                .email(email)
                .password(password)
                .active(true)
                .roles(Set.of(Role.ROLE_USER))
                .build();

        when(authenticationManager.authenticate(any()))
                .thenReturn(mock(Authentication.class));
        when(userRepository.findByEmailIgnoreCase(email))
                .thenReturn(Optional.of(user));

        assertThrows(AccessDeniedException.class,
                () -> userService.loginAdmin(request));

        verify(jwtService, never()).generateAdminToken(any());
    }

    @Test
    @DisplayName("Should throw exception when admin login authentication fails")
    void shouldThrowExceptionWhenAdminLoginAuthenticationFails() {
        String email = "admin@email.com";
        String password = "wrongPassword";
        UserLoginRequest request = new UserLoginRequest(email, password);

        when(authenticationManager.authenticate(any()))
                .thenThrow(new RuntimeException("Bad credentials"));

        assertThrows(RuntimeException.class,
                () -> userService.loginAdmin(request));

        verify(userRepository, never()).findByEmailIgnoreCase(any());
        verify(jwtService, never()).generateAdminToken(any());
    }

    @Test
    @DisplayName("Should successfully update user's email")
    void shouldUpdateEmail() {
        String email = "user@email.com";
        String newEmail = "NEW@email.com";
        String normalized = "new@email.com";
        String password = "password";
        Long userId = 1L;

        User user = User.builder()
                .id(userId)
                .email(email)
                .password(password)
                .active(true)
                .roles(Set.of(Role.ROLE_USER))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase(normalized)).thenReturn(false);

        userService.updateEmail(userId, newEmail);

        verify(userRepository).save(argThat(saved ->
                saved.getEmail().equals(normalized)
        ));
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when updating email for non-existing user")
    void shouldThrowExceptionWhenUpdatingEmailForNonExistingUser() {
        Long userId = 999L;
        String newEmail = "new@email.com";

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.updateEmail(userId, newEmail));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw EmailAlreadyTakenException when updating to existing email")
    void shouldThrowExceptionWhenUpdatingToTakenEmail() {
        Long userId = 1L;
        String currentEmail = "current@email.com";
        String newEmail = "taken@email.com";
        String normalized = "taken@email.com";

        User user = User.builder()
                .id(userId)
                .email(currentEmail)
                .password("password")
                .active(true)
                .roles(Set.of(Role.ROLE_USER))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase(normalized)).thenReturn(true);

        assertThrows(EmailAlreadyTakenException.class,
                () -> userService.updateEmail(userId, newEmail));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should not check email availability when updating to same email")
    void shouldNotCheckAvailabilityWhenUpdatingToSameEmail() {
        Long userId = 1L;
        String email = "test@email.com";
        String normalized = "test@email.com";

        User user = User.builder()
                .id(userId)
                .email(normalized)
                .password("password")
                .active(true)
                .roles(Set.of(Role.ROLE_USER))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.updateEmail(userId, "TEST@email.com");

        verify(userRepository).save(argThat(saved ->
                saved.getEmail().equals(normalized)
        ));
        verify(userRepository, never()).existsByEmailIgnoreCase(any());
    }

    @Test
    @DisplayName("Should throw EmailAlreadyTakenException when DataIntegrityViolationException occurs during email update")
    void shouldThrowEmailAlreadyTakenExceptionWhenDataIntegrityViolationDuringEmailUpdate() {
        Long userId = 1L;
        String newEmail = "new@email.com";

        User user = User.builder()
                .id(userId)
                .email("old@email.com")
                .password("password")
                .active(true)
                .roles(Set.of(Role.ROLE_USER))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase(any())).thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        assertThrows(EmailAlreadyTakenException.class,
                () -> userService.updateEmail(userId, newEmail));
    }

    @Test
    @DisplayName("Should successfully update email by admin")
    void shouldUpdateEmailByAdmin() {
        Long adminId = 1L;
        Long userId = 2L;
        String currentEmail = "old@email.com";
        String newEmail = "NEW@email.com";
        String normalized = "new@email.com";

        User user = User.builder()
                .id(userId)
                .email(currentEmail)
                .password("password")
                .active(true)
                .roles(Set.of(Role.ROLE_USER))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase(normalized)).thenReturn(false);

        userService.updateEmailById(adminId, userId, newEmail);

        verify(userRepository).save(argThat(saved ->
                saved.getEmail().equals(normalized)
        ));
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when admin updates email for non-existing user")
    void shouldThrowExceptionWhenAdminUpdatesEmailForNonExistingUser() {
        Long adminId = 1L;
        Long userId = 999L;
        String newEmail = "new@email.com";

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.updateEmailById(adminId, userId, newEmail));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw EmailAlreadyTakenException when DataIntegrityViolationException occurs during admin email update")
    void shouldThrowEmailAlreadyTakenExceptionWhenDataIntegrityViolationDuringAdminEmailUpdate() {
        Long adminId = 1L;
        Long userId = 2L;
        String newEmail = "new@email.com";

        User user = User.builder()
                .id(userId)
                .email("old@email.com")
                .password("password")
                .active(true)
                .roles(Set.of(Role.ROLE_USER))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase(any())).thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        assertThrows(EmailAlreadyTakenException.class,
                () -> userService.updateEmailById(adminId, userId, newEmail));
    }

    @Test
    @DisplayName("Should successfully update user's password")
    void shouldUpdatePassword() {
        String email = "user@email.com";
        String password = "password";
        String newPassword = "newPassword";
        String newPasswordEncoded = "newEncoded";
        PasswordUpdateRequest request = new PasswordUpdateRequest(password, newPassword);
        Long userId = 1L;

        User user = User.builder()
                .id(userId)
                .email(email)
                .password(password)
                .active(true)
                .roles(Set.of(Role.ROLE_USER))
                .build();

        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(newPassword)).thenReturn(newPasswordEncoded);

        userService.updatePassword(userId, request);

        verify(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        verify(userRepository).save(argThat(saved ->
                saved.getPassword().equals(newPasswordEncoded))
        );
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when updating password for non-existing user")
    void shouldThrowExceptionWhenUpdatingPasswordForNonExistingUser() {
        Long userId = 999L;
        PasswordUpdateRequest request = new PasswordUpdateRequest("old", "new");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.updatePassword(userId, request));

        verify(authenticationManager, never()).authenticate(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when password authentication fails during password update")
    void shouldThrowExceptionWhenPasswordAuthenticationFailsDuringPasswordUpdate() {
        Long userId = 1L;
        String currentPassword = "wrongPassword";
        String newPassword = "newPassword";
        PasswordUpdateRequest request = new PasswordUpdateRequest(currentPassword, newPassword);

        User user = User.builder()
                .id(userId)
                .email("test@email.com")
                .password("actualPassword")
                .active(true)
                .roles(Set.of(Role.ROLE_USER))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any()))
                .thenThrow(new RuntimeException("Bad credentials"));

        assertThrows(RuntimeException.class,
                () -> userService.updatePassword(userId, request));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully update password by admin")
    void shouldUpdatePasswordByAdmin() {
        Long adminId = 1L;
        Long userId = 2L;
        String newPassword = "newPassword";
        String encodedPassword = "encoded";

        User user = User.builder()
                .id(userId)
                .email("test@email.com")
                .password("oldPassword")
                .active(true)
                .roles(Set.of(Role.ROLE_USER))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);

        userService.updatePasswordById(adminId, userId, newPassword);

        verify(userRepository).save(argThat(saved ->
                saved.getPassword().equals(encodedPassword)
        ));
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when admin updates password for non-existing user")
    void shouldThrowExceptionWhenAdminUpdatesPasswordForNonExistingUser() {
        Long adminId = 1L;
        Long userId = 999L;
        String newPassword = "newPassword";

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.updatePasswordById(adminId, userId, newPassword));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully find user by email")
    void shouldFindUserByEmail() {
        String email = "TEST@email.com";
        String normalized = "test@email.com";
        User user = User.builder()
                .id(1L)
                .email(normalized)
                .password("password")
                .active(true)
                .roles(Set.of(Role.ROLE_USER))
                .build();

        when(userRepository.findByEmailIgnoreCase(normalized))
                .thenReturn(Optional.of(user));

        User result = userService.findByEmail(email);

        assertNotNull(result);
        assertEquals(normalized, result.getEmail());
        verify(userRepository).findByEmailIgnoreCase(normalized);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when user not found by email")
    void shouldThrowExceptionWhenUserNotFoundByEmail() {
        String email = "notfound@email.com";
        String normalized = "notfound@email.com";

        when(userRepository.findByEmailIgnoreCase(normalized))
                .thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.findByEmail(email));
    }

    @Test
    @DisplayName("Should successfully find user by email mapped")
    void shouldFindUserByEmailMapped() {
        String email = "test@email.com";
        User user = User.builder()
                .id(1L)
                .email(email)
                .password("password")
                .active(true)
                .roles(Set.of(Role.ROLE_USER))
                .build();

        when(userRepository.findByEmailIgnoreCase(email))
                .thenReturn(Optional.of(user));

        UserResponse response = userService.findByEmailMapped(email);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(email, response.email());
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when user not found by email mapped")
    void shouldThrowExceptionWhenUserNotFoundByEmailMapped() {
        String email = "notfound@email.com";

        when(userRepository.findByEmailIgnoreCase(any()))
                .thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.findByEmailMapped(email));
    }

    @Test
    @DisplayName("Should successfully find user by id")
    void shouldFindUserById() {
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("test@email.com")
                .password("password")
                .active(true)
                .roles(Set.of(Role.ROLE_USER))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User result = userService.findById(userId);

        assertNotNull(result);
        assertEquals(userId, result.getId());
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when user not found by id")
    void shouldThrowExceptionWhenUserNotFoundById() {
        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.findById(userId));
    }

    @Test
    @DisplayName("Should successfully find user by id mapped")
    void shouldFindUserByIdMapped() {
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("test@email.com")
                .password("password")
                .active(true)
                .roles(Set.of(Role.ROLE_USER))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse response = userService.findByIdMapped(userId);

        assertNotNull(response);
        assertEquals(userId, response.id());
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when user not found by id mapped")
    void shouldThrowExceptionWhenUserNotFoundByIdMapped() {
        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.findByIdMapped(userId));
    }

    @Test
    @DisplayName("Should successfully get own user info")
    void shouldGetOwnUserInfo() {
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("test@email.com")
                .password("password")
                .active(true)
                .roles(Set.of(Role.ROLE_USER))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse response = userService.getOwnUserInfo(userId);

        assertNotNull(response);
        assertEquals(userId, response.id());
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when getting own user info for non-existing user")
    void shouldThrowExceptionWhenGettingOwnUserInfoForNonExistingUser() {
        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.getOwnUserInfo(userId));
    }

    @Test
    @DisplayName("Should successfully get all users")
    void shouldGetAllUsers() {
        User user1 = User.builder()
                .id(1L)
                .email("user1@email.com")
                .password("password")
                .active(true)
                .roles(Set.of(Role.ROLE_USER))
                .build();

        User user2 = User.builder()
                .id(2L)
                .email("user2@email.com")
                .password("password")
                .active(true)
                .roles(Set.of(Role.ROLE_ADMIN))
                .build();

        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        List<UserResponse> responses = userService.getAllUsers();

        assertNotNull(responses);
        assertEquals(2, responses.size());
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("Should successfully delete user by admin")
    void shouldDeleteUserByAdmin() {
        Long adminId = 1L;
        Long userId = 2L;
        when(userRepository.existsById(userId)).thenReturn(true);

        userService.deleteUser(adminId, userId);

        verify(userRepository).deleteById(userId);
    }

    @Test
    @DisplayName("Should successfully delete all users")
    void shouldDeleteAllUsers() {
        userService.deleteAllUsers();

        verify(userRepository).deleteAll();
    }
}