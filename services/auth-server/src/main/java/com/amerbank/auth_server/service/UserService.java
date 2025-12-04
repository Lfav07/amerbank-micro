package com.amerbank.auth_server.service;

import com.amerbank.auth_server.dto.PasswordUpdateRequest;
import com.amerbank.auth_server.exception.EmailAlreadyTakenException;
import com.amerbank.auth_server.exception.UserNotFoundException;
import com.amerbank.auth_server.model.User;
import com.amerbank.auth_server.repository.UserRepository;
import com.amerbank.auth_server.security.JwtService;
import com.amerbank.common_dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
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
        return userRepository.existsByEmailIgnoreCase(email.trim().toLowerCase());
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
    public void registerUser(UserRegisterRequest request) {
        String maskedEmail = maskEmail(request.email());
        log.info("Attempting to register new user with email {} ...", maskedEmail);

        if (isEmailTaken(request.email())) {
            log.warn("Registration failed: email {} is already taken.", maskedEmail);
            throw new EmailAlreadyTakenException("Email already taken");
        }

        User user = User.builder()
                .email(request.email().trim().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .roles(Set.of(Role.ROLE_USER))
                .active(true)
                .build();

        log.debug("User entity prepared for registration: {}", user);

        userRepository.save(user);

        log.info("User successfully registered with email {}", maskedEmail);
    }

    /**
     * Registers a new admin user with the ROLE_ADMIN role.
     *
     * @param request the registration request containing email and password
     * @throws EmailAlreadyTakenException if the email is already in use
     */
    public void registerAdmin(UserRegisterRequest request) {
        String maskedEmail = maskEmail(request.email());

        if (isEmailTaken(request.email())) {
            throw new EmailAlreadyTakenException("Email already taken");
        }

        User user = User.builder()
                .email(request.email().trim().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .roles(Set.of(Role.ROLE_ADMIN))
                .active(true)
                .build();
        log.debug("Admin entity prepared for registration: {}", user);

        userRepository.save(user);
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
    public void authenticate(String email, String password) {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(email, password);
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
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = findByEmail(request.email());
        Long customerId = customerServiceClient.getCustomerIdByUserId(user.getId());
        String token = jwtService.generateToken(user, customerId);
        log.info("User successfully logged in with email {}", maskedEmail);
        return new AuthenticationResponse(token);
    }

    /**
     * Logs-in an admin and creates a jwt token.
     *
     * @param request the login request containing email and password
     * @return AuthenticationResponse containing jwt token
     * @throws UserNotFoundException if the user is not found.
     */
    public AuthenticationResponse loginAdmin(UserLoginRequest request) {
        String maskedEmail = maskEmail(request.email());
        log.debug("Attempting to log in admin with email {}", maskedEmail);
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = findByEmail(request.email());
        String token = jwtService.generateAdminToken(user);
        log.info("Admin successfully logged in with email {}", maskedEmail);
        return new AuthenticationResponse(token);
    }


    // -------------------------------------------------------------------------
    // Updates
    // -------------------------------------------------------------------------

    /**
     * Updates the email address of a user.
     *
     * @param id    the ID of the user to update
     * @param email the new email address
     * @throws UserNotFoundException if no user with the given ID exists
     */
    @Transactional
    public void updateEmail(Long id, String email) {
        String maskedEmail = maskEmail(email);
        User user = findById(id);
        user.setEmail(email.trim().toLowerCase());
        log.info("User with id {} successfully updated their email to {}", id, maskedEmail);
        userRepository.save(user);
    }

    /**
     * Updates the email address of a user.
     *
     * @param adminId the ID of the current logged in admin
     * @param id      the ID of the user to update
     * @param email   the new email address
     * @throws UserNotFoundException if no user with the given ID exists
     */
    @Transactional
    public void updateEmailById(Long adminId, Long id, String email) {
        User user = findById(id);
        user.setEmail(email.trim().toLowerCase());
        log.info("Admin with id {} successfully updated user with id {} 's email", adminId, id);
        userRepository.save(user);
    }

    /**
     * Updates the password of a user.
     *
     * @param email   the email of the user to update
     * @param request the old and new password
     * @throws UserNotFoundException if no user with the given ID exists
     */
    @Transactional
    public void updatePassword(String email, PasswordUpdateRequest request) {
        authenticate(email, request.oldPassword());
        User user = findByEmail(email);
        String password = passwordEncoder.encode(request.newPassword());
        user.setPassword(password);
        log.info("User with id {} successfully updated their password", user.getId());
        userRepository.save(user);
    }

    /**
     * Updates the password of a user.
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
     * Retrieves all users from the database.
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }


    // -------------------------------------------------------------------------
    // Deletion
    // -------------------------------------------------------------------------

    /**
     * Deletes a user by ID.
     *
     * @param adminId the ID of the current logged in admin
     * @param id      the ID of the user to delete
     * @throws UserNotFoundException if the user does not exist
     */
    @Transactional
    public void deleteUser(Long adminId, Long id) throws UserNotFoundException {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found");
        }

        log.info("Admin with id {} successfully deleted user with id {}", adminId, id);
        userRepository.deleteById(id);
    }

    /**
     * Deletes all users from the database.
     * Demo only.
     */
    public void deleteAllUsers() {
        userRepository.deleteAll();
    }


    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int at = email.indexOf('@');
        return email.substring(0, Math.min(2, at)) + "***" + email.substring(at);
    }
    
    // -------------------------------------------------------------------------
    // Kafka Listener
    // -------------------------------------------------------------------------

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
