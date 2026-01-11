package com.amerbank.auth_server.service;

import com.amerbank.auth_server.dto.*;
import com.amerbank.auth_server.exception.EmailAlreadyTakenException;
import com.amerbank.auth_server.exception.UserNotFoundException;
import com.amerbank.auth_server.model.User;
import com.amerbank.auth_server.repository.UserRepository;
import com.amerbank.auth_server.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private  final UserMapper mapper;


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

    /**
     * Checks whether a user with the given ID exists.
     *
     * @param id the user ID to check
     * @return true if the user exists; false otherwise
     */
    public boolean existsById(Long id) {
        return userRepository.existsById(id);
    }


    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    /**
     * Registers a new user with the ROLE_USER role.
     *
     * @param request the registration request containing email and password
     * @throws EmailAlreadyTakenException if the email is already in use
     */
    public UserResponse registerUser(UserRegisterRequest request) {
        String maskedEmail = maskEmail(request.email());
        log.info("Attempting to register new user with email {} ...", maskedEmail);

        if (isEmailTaken(request.email())) {
            log.warn("User registration failed: email {} is already taken.", maskedEmail);
            throw new EmailAlreadyTakenException("Email already taken");
        }

        User user = User.builder()
                .email(normalizeEmail(request.email()))
                .password(passwordEncoder.encode(request.password()))
                .roles(Set.of(Role.ROLE_USER))
                .active(true)
                .build();

        log.debug("User entity prepared for registration: {}", user);

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyTakenException("Email already taken");
        }
        log.info("User successfully registered with email {}", maskedEmail);
        return mapper.toResponse(user);
    }

    /**
     * Registers a new admin user with the ROLE_ADMIN role.
     * Enabled for demonstrative purposes.
     * @param request the registration request containing email and password
     * @throws EmailAlreadyTakenException if the email is already in use
     */
    public void registerAdmin(UserRegisterRequest request) {
        String maskedEmail = maskEmail(request.email());
        log.info("Attempting to register new admin with email {} ...", maskedEmail);

        if (isEmailTaken(request.email())) {
            log.warn("Admin registration failed: email {} is already taken.", maskedEmail);
            throw new EmailAlreadyTakenException("Email already taken");
        }

        User user = User.builder()
                .email(normalizeEmail(request.email()))
                .password(passwordEncoder.encode(request.password()))
                .roles(Set.of(Role.ROLE_ADMIN))
                .active(true)
                .build();
        log.debug("Admin entity prepared for registration: {}", user);

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyTakenException("Email already taken");
        }

        log.info("Admin successfully registered with email {}", maskedEmail);
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
        String maskedEmail = maskEmail(request.email());
        log.debug("Attempting to log in user with email {}", maskedEmail);

        String email = normalizeEmail(request.email());
        authenticate(email, request.password());
        User user = findByEmail(email);

        Long customerId = customerServiceClient.getCustomerIdByUserId(user.getId());
        String token = jwtService.generateToken(user, customerId);
        log.info("User successfully logged in with email {}", maskedEmail);
        return new AuthenticationResponse(token);
    }

    /**
     * Logs-in an admin and creates a jwt token.
     * Enabled for demonstrative purposes.
     * @param request the login request containing email and password
     * @return AuthenticationResponse containing jwt token
     * @throws UserNotFoundException if the user is not found.
     */
    public AuthenticationResponse loginAdmin(UserLoginRequest request) {
        String maskedEmail = maskEmail(request.email());
        log.debug("Attempting to log in admin with email {}", maskedEmail);


        String email = normalizeEmail(request.email());
        authenticate(email, request.password());
        User user = findByEmail(email);
        if (!user.getRoles().contains(Role.ROLE_ADMIN)) {
            throw new AccessDeniedException("Not an admin");
        }

        String token = jwtService.generateAdminToken(user);
        log.info("Admin successfully logged in with email {}", maskedEmail);
        return new AuthenticationResponse(token);
    }


    // -------------------------------------------------------------------------
    // Updates
    // -------------------------------------------------------------------------

    /**
     * Updates the email address of a user.
     * Regular customer use only.
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
        log.info("User with id {} successfully updated their email to {}", id, maskEmail(normalizedEmail));
    }


    /**
     * Updates the email address of a user by their id.
     * Admin use only.
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
        log.info("Admin with id {} successfully updated user with id {} 's email", adminId, id);
    }

    /**
     * Updates the password of a user.
     * Regular customer use only.
      * @param id the id of the user to update
     * @param request the old and new password
     * @throws UserNotFoundException if no user with the given ID exists
     */
    @Transactional
    public void updatePassword(Long id, PasswordUpdateRequest request) {
        User user = findById(id);
        String normalizedEmail = normalizeEmail(user.getEmail());
        authenticate(normalizedEmail, request.currentPassword());
        String password = passwordEncoder.encode(request.newPassword());
        user.setPassword(password);
        log.info("User with id {} successfully updated their password", user.getId());
        userRepository.save(user);
    }

    /**
     * Updates the password of a user.
     * Admin use only.
     * @param adminId     the ID of the current logged in admin
     * @param id          the id of the user to update
     * @param newPassword the new password
     * @throws UserNotFoundException if no user with the given ID exists
     */
    @Transactional
    public void updatePasswordById(Long adminId, Long id, String newPassword) {
        User user = findById(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        log.info("Admin with id {} successfully updated user with id {} 's password", adminId, id);
        userRepository.save(user);
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
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UserNotFoundException("User not found!"));
    }

    /**
     * Finds a user by email (case-insensitive).
     * Maps the user to userResponse.
     * @param email the email to search for
     * @return the found User entity
     * @throws UserNotFoundException if no user with the given email exists
     */
    public UserResponse findByEmailMapped (String email) {
        return userRepository.findByEmailIgnoreCase(email).map(mapper::toResponse)
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
     *  Maps the user to userResponse.
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
        log.info("Admin with id {} successfully deleted user with id {}", adminId, id);
        userRepository.deleteById(id);
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
     * @param email the email to be masked.
     * @return the user's email in its normalized state.
     */
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
    
    // -------------------------------------------------------------------------
    // Kafka Listener
    // -------------------------------------------------------------------------

    // When a customer is deleted from the database, the associated user must be deleted too.
    /**
     * Handles a customer deletion event.
     * Uses Kafka to receive customer deletion events.
     * Deletes the user associated with the customer.
     * @param event the customer deletion event received from Kafka.
     */
    @KafkaListener(topics = "customer.deleted", groupId = "auth-service")
    @Transactional
    public void handleCustomerDeleted(CustomerDeletedEvent event) {
        if (!userRepository.existsById(event.getUserId())) {
            log.warn("Received customer.deleted event for non-existing user {}", event.getUserId());
            return;
        }
        userRepository.deleteById(event.getUserId());
        log.info("User with id {} successfully deleted by customer deletion", event.getUserId());
    }
}
