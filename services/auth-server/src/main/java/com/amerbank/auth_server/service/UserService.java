package com.amerbank.auth_server.service;

import com.amerbank.auth_server.dto.*;
import com.amerbank.auth_server.exception.*;
import com.amerbank.auth_server.model.User;
import com.amerbank.auth_server.repository.UserRepository;
import com.amerbank.auth_server.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service class for handling user-related operations such as registration,
 * update, lookup, and deletion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CustomerServiceClient customerServiceClient;
    private final JwtService jwtService;
    private final UserMapper mapper;


    // -------------------------------------------------------------------------
    // Checkers
    // -------------------------------------------------------------------------

    /**
     * Checks if the given email is already taken (case-insensitive).
     *
     * @param email the email to check
     * @return true if the email is already registered; false otherwise
     */
    public boolean isEmailTaken(String email) {
        return userRepository.existsByEmailIgnoreCase(normalizeEmail(email));
    }


    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    public UserResponse registerUser(UserRegisterRequest request) {

        log.debug("Processing new user registration");

        if (isEmailTaken(request.email())) {
            throw new EmailAlreadyTakenException("Email already taken");
        }

        User user = createUserTransactional(request);

        try {

            Long customerId = registerCustomerExternal(request, user.getId());

            updateUserCustomerIdTransactional(user.getId(), customerId);

            User updatedUser = userRepository.findById(user.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "User disappeared after registration: " + user.getId()));

            log.info("User successfully registered with id={} and customerId={}",
                    updatedUser.getId(), customerId);

            return mapper.toResponse(updatedUser);

        } catch (CustomerServiceUnavailableException ex) {

            log.error("Customer service unavailable for userId={}", user.getId(), ex);
            deleteUserTransactional(user.getId());
            throw ex;

        } catch (CustomerRegistrationFailedException ex) {

            log.warn("Customer registration failed for userId={}", user.getId(), ex);
            deleteUserTransactional(user.getId());
            throw ex;

        } catch (RuntimeException ex) {

            log.error("Unexpected error during registration for userId={}", user.getId(), ex);
            deleteUserTransactional(user.getId());
            throw new RegistrationFailedException("Unexpected error during registration", ex);
        }
    }


    @Transactional
    public User createUserTransactional(UserRegisterRequest request) {

        User user = User.builder()
                .email(normalizeEmail(request.email()))
                .password(passwordEncoder.encode(request.password()))
                .roles(Set.of(Role.ROLE_USER))
                .active(true)
                .build();

        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyTakenException("Email already taken");
        }
    }

    @Transactional
    public void updateUserCustomerIdTransactional(Long userId, Long customerId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setCustomerId(customerId);
        userRepository.save(user);
    }



    private Long registerCustomerExternal(UserRegisterRequest request, Long userId) {

        CustomerRegistrationRequest customerRequest =
                new CustomerRegistrationRequest(
                        request.firstName(),
                        request.lastName(),
                        userId,
                        request.dateOfBirth()
                );

        CustomerRegistrationResponse response =
                customerServiceClient.registerCustomer(customerRequest);

        if (response == null || response.id() == null) {
            throw new CustomerRegistrationFailedException("Customer service returned invalid response");
        }

        return response.id();
    }



    /**
     * Registers a new admin user with the ROLE_ADMIN role.
     * Enabled for demonstrative purposes.
     *
     * @param request the registration request containing email and password
     * @throws EmailAlreadyTakenException if the email is already in use
     */
    public UserResponse registerAdmin(AdminRegisterRequest request) {
        log.debug("Processing new admin registration");

        if (isEmailTaken(request.email())) {
            log.warn("Admin registration failed: email already taken");
            throw new EmailAlreadyTakenException("Email already taken");
        }

        User user = User.builder()
                .email(normalizeEmail(request.email()))
                .password(passwordEncoder.encode(request.password()))
                .roles(new HashSet<>(Set.of(Role.ROLE_ADMIN)))
                .active(true)
                .build();
        log.debug("Admin entity prepared for registration");

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyTakenException("Email already taken");
        }

        log.info("New admin account registered successfully");
        return mapper.toResponse(user);
    }


    // -------------------------------------------------------------------------
    // Authentication & Login
    // -------------------------------------------------------------------------

    /**
     * Authenticates a user if the password and email are correct.
     *
     * @param email    The user's email.
     * @param password The user's password.
     */
    private void authenticate(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(normalizedEmail, password);
        authenticationManager.authenticate(token);
    }


    /**
     * Logs-in a user and creates a jwt token.
     *
     * @param request the login request containing email and password
     * @return AuthenticationResponse containing jwt token
     * @throws UserNotFoundException if the user is not found.
     */
    public AuthenticationResponse login(UserLoginRequest request) {
        log.debug("Processing user login attempt");

        String email = normalizeEmail(request.email());
        try {
            authenticate(email, request.password());
        } catch (Exception ex) {
            log.warn("User login failed: authentication failed");
            throw ex;
        }
        User user = findByEmail(email);

        Long customerId = user.getCustomerId();
        String token = jwtService.generateToken(user, customerId);
        log.info("User account logged in successfully");
        return new AuthenticationResponse(token);
    }

    /**
     * Logs-in an admin and creates a jwt token.
     * Enabled for demonstrative purposes.
     *
     * @param request the login request containing email and password
     * @return AuthenticationResponse containing jwt token
     * @throws UserNotFoundException if the user is not found.
     */
    public AuthenticationResponse loginAdmin(UserLoginRequest request) {
        log.debug("Processing admin login attempt");


        String email = normalizeEmail(request.email());
        try {
            authenticate(email, request.password());
        } catch (Exception ex) {
            log.warn("Admin login failed: authentication failed");
            throw ex;
        }
        User user = findByEmail(email);
        if (!user.getRoles().contains(Role.ROLE_ADMIN)) {
            log.warn("Admin login denied: insufficient privileges");
            throw new AccessDeniedException("Not an admin");
        }

        String token = jwtService.generateAdminToken(user);
        log.info("Admin account logged in successfully");
        return new AuthenticationResponse(token);
    }


    // -------------------------------------------------------------------------
    // Updates
    // -------------------------------------------------------------------------

    /**
     * Updates the email address of a user.
     * Regular customer use only.
     *
     * @param id    the ID of the user to update
     * @param email the new email address
     * @throws UserNotFoundException if no user with the given ID exists
     */
    @Transactional
    public void updateEmail(Long id, String email) {
        String normalizedEmail = normalizeEmail(email);

        User user = findById(id);

        if (!user.getEmail().equals(normalizedEmail) && isEmailTaken(normalizedEmail)) {
            throw new EmailAlreadyTakenException("Email already taken");
        }

        user.setEmail(normalizedEmail);
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyTakenException("Email already taken");
        }
        log.info("User account email updated successfully");
    }


    /**
     * Updates the email address of a user by their id.
     * Admin use only.
     *
     * @param adminId the ID of the current logged in admin
     * @param id      the ID of the user to update
     * @param email   the new email address
     * @throws UserNotFoundException if no user with the given ID exists
     */
    @Transactional
    public void updateEmailById(Long adminId, Long id, String email) {
        String normalizedEmail = normalizeEmail(email);

        User user = findById(id);
        if (!user.getEmail().equals(normalizedEmail) && isEmailTaken(normalizedEmail)) {
            throw new EmailAlreadyTakenException("Email already taken");
        }
        user.setEmail(normalizedEmail);
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyTakenException("Email already taken");
        }
        log.debug("Admin with id {} updated user {} email to {}", adminId, id, maskEmail(normalizedEmail));
        log.info("User account email updated successfully by admin");
    }

    /**
     * Updates the password of a user.
     * Regular customer use only.
     *
     * @param id      the id of the user to update
     * @param request the old and new password
     * @throws UserNotFoundException if no user with the given ID exists
     */
    @Transactional
    public void updatePassword(Long id, PasswordUpdateRequest request) {
        User user = findById(id);
        String normalizedEmail = normalizeEmail(user.getEmail());

        try {
            authenticate(normalizedEmail, request.currentPassword());
        } catch (Exception ex) {
            log.warn("User password update failed: authentication failed");
            throw ex;
        }


        String password = passwordEncoder.encode(request.newPassword());

        user.setPassword(password);
        userRepository.save(user);

        log.info("User account credentials updated successfully");
    }

    /**
     * Updates the password of a user.
     * Admin use only.
     *
     * @param adminId     the ID of the current logged in admin
     * @param id          the id of the user to update
     * @param newPassword the new password
     * @throws UserNotFoundException if no user with the given ID exists
     */
    @Transactional
    public void updatePasswordById(Long adminId, Long id, String newPassword) {
        User user = findById(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("User account credentials updated successfully");
    }


    // -------------------------------------------------------------------------
    // Retrieval
    // -------------------------------------------------------------------------

    /**
     * Finds a user by email (case-insensitive).
     *
     * @param email the email to search for
     * @return the found User entity
     * @throws UserNotFoundException if no user with the given email exists
     */
    public User findByEmail(String email) {
        String normalized = normalizeEmail(email);
        return userRepository.findByEmailIgnoreCase(normalized)
                .orElseThrow(() -> new UserNotFoundException("User not found!"));
    }

    /**
     * Finds a user by email (case-insensitive).
     * Maps the user to userResponse.
     *
     * @param email the email to search for
     * @return the found User entity
     * @throws UserNotFoundException if no user with the given email exists
     */
    public UserResponse findByEmailMapped(String email) {
        String normalized = normalizeEmail(email);
        return userRepository.findByEmailIgnoreCase(normalized).map(mapper::toResponse)
                .orElseThrow(() -> new UserNotFoundException("User not found!"));
    }

    /**
     * Finds a user by ID.
     *
     * @param id the user ID to search for
     * @return the found User entity
     * @throws UserNotFoundException if no user with the given ID exists
     */
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found!"));
    }

    /**
     * Finds a user by ID.
     * Maps the user to userResponse.
     *
     * @param id the user ID to search for
     * @return the found User entity
     * @throws UserNotFoundException if no user with the given ID exists
     */
    public UserResponse findByIdMapped(Long id) {
        return userRepository.findById(id).map(mapper::toResponse)
                .orElseThrow(() -> new UserNotFoundException("User not found!"));
    }

    public UserResponse getOwnUserInfo(Long id) {
        return mapper.toResponse(findById(id));
    }

    /**
     * Retrieves all users from the database.
     * Demonstrative use only.
     */
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(mapper::toResponse).toList();
    }


    // -------------------------------------------------------------------------
    // Deletion
    // -------------------------------------------------------------------------

    // Hard deletion implemented for ease-to-use demonstrative purpose.

    /**
     * Deletes a user by ID.
     *
     * @param adminId the ID of the current logged in admin
     * @param id      the ID of the user to delete
     * @throws UserNotFoundException if the user does not exist
     */
    @Transactional
    public void deleteUser(Long adminId, Long id) throws UserNotFoundException {
        userRepository.deleteById(id);
        log.debug("Admin with id {} deleted user with id {}", adminId, id);
        log.info("User account deleted successfully");
    }

    @Transactional
    public void deleteUserTransactional(Long userId) {
        userRepository.deleteById(userId);
    }

    //  Demonstrative use only

    /**
     * Deletes all users from the database.
     */
    public void deleteAllUsers() {
        userRepository.deleteAll();
    }


    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    /**
     * Masks a user's email.
     *
     * @param email the email to be masked.
     * @return the user's email in its masked state.
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int at = email.indexOf('@');
        return email.substring(0, Math.min(2, at)) + "***" + email.substring(at);
    }

    /**
     * Normalizes a user's email.
     *
     * @param email the email to be masked.
     * @return the user's email in its normalized state.
     */
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
