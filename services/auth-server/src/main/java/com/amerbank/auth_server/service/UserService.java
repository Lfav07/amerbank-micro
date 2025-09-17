package com.amerbank.auth_server.service;

import com.amerbank.auth_server.dto.PasswordUpdateRequest;
import com.amerbank.auth_server.exception.EmailAlreadyTakenException;
import com.amerbank.auth_server.exception.UserNotFoundException;
import com.amerbank.auth_server.model.User;
import com.amerbank.auth_server.repository.UserRepository;
import com.amerbank.auth_server.security.JwtService;
import com.amerbank.common_dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Service class for handling user-related operations such as registration,
 * update, lookup, and deletion.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private  final AuthenticationManager authenticationManager;
    private  final CustomerServiceClient customerServiceClient;
    private  final JwtService jwtService;

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
     * Registers a new user with the ROLE_USER role.
     *
     * @param request the registration request containing email and password
     * @return the saved User entity
     * @throws EmailAlreadyTakenException if the email is already in use
     */
    public User registerUser(UserRegisterRequest request) {
        if (isEmailTaken(request.email())) {
            throw new EmailAlreadyTakenException("Email already taken");
        }

        User user = User.builder()
                .email(request.email().trim().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .roles(Set.of(Role.ROLE_USER))
                .active(true)
                .build();

        return userRepository.save(user);
    }
    /**
     * Logs-in a user and creates a jwt token.
     * @param request the login request containing email and password
     * @return AuthenticationResponse containing jwt token
     * @throws UserNotFoundException if the user is not found.
     */
    public AuthenticationResponse login(UserLoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = findByEmail(request.email());
        Long customerId = customerServiceClient.getCustomerIdByUserId(user.getId());
        String token = jwtService.generateToken(user, customerId);

        return new AuthenticationResponse(token);
    }
    /**
     * Logs-in an admin and creates a jwt token.
     * @param request the login request containing email and password
     * @return AuthenticationResponse containing jwt token
     * @throws UserNotFoundException if the user is not found.
     */
    public AuthenticationResponse loginAdmin(UserLoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = findByEmail(request.email());
        String token = jwtService.generateAdminToken(user);

        return new AuthenticationResponse(token);
    }


    /**
     * Authenticates a user if the password and email are correct.
     * @param email The user's email.
     * @param password The user's password.
     */
    public void  authenticate(String email, String password) {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(email, password);
        authenticationManager.authenticate(token);
    }

    /**
     * Registers a new admin user with the ROLE_ADMIN role.
     *
     * @param request the registration request containing email and password
     * @return the saved User entity
     * @throws EmailAlreadyTakenException if the email is already in use
     */
    public User registerAdmin(UserRegisterRequest request) {
        if (isEmailTaken(request.email())) {
            throw new EmailAlreadyTakenException("Email already taken");
        }

        User user = User.builder()
                .email(request.email().trim().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .roles(Set.of(Role.ROLE_ADMIN))
                .active(true)
                .build();

        return userRepository.save(user);
    }

    /**
     * Updates the email address of a user.
     *
     * @param id the ID of the user to update
     * @param email the new email address
     * @throws UserNotFoundException if no user with the given ID exists
     */
    public void updateEmail(Long id, String email) {
        User user = findById(id);
        user.setEmail(email);
        userRepository.save(user);
    }

    /**
     * Updates the email address of a user.
     *
     * @param id the ID of the user to update
     * @param email the new email address
     * @throws UserNotFoundException if no user with the given ID exists
     */
    public void updateEmailById(Long id, String email) {
        User user = findById(id);
        userRepository.save(user);
    }


    /**
     * Updates the password of a user.
     *
     * @param email the email of the user to update
     * @param request the old and new password
     * @throws UserNotFoundException if no user with the given ID exists
     */
    public void updatePassword(String email, PasswordUpdateRequest request) {

        authenticate(email, request.oldPassword());
        User user = findByEmail(email);
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    /**
     * Updates the password of a user.
     *
     * @param id the id of the user to update
     * @param newPassword the new password
     * @throws UserNotFoundException if no user with the given ID exists
     */
    @Transactional
    public void updatePasswordById(Long id, String newPassword) {
        User user = findById(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }


    /**
     * Deletes a user by ID.
     *
     * @param id the ID of the user to delete
     * @throws UserNotFoundException if the user does not exist
     */
    public void deleteUser(Long id) throws UserNotFoundException {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found");
        }
        userRepository.deleteById(id);
    }

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

    @KafkaListener(topics = "customer.deleted", groupId = "auth-service")
    public void handleCustomerDeleted(CustomerDeletedEvent event) {
        userRepository.deleteById(event.getUserId());
    }

    /**
     * Finds a user by ID.
     *
     * @param id the user ID to search for
     * @return the found User entity
     * @throws UserNotFoundException if no user with the given ID exists
     */
    public boolean existsById(Long id) {
        return userRepository.existsById(id);
    }

    /**
     * Deletes all users from the database.
     */
    public void deleteAllUsers() {
        userRepository.deleteAll();
    }
}
