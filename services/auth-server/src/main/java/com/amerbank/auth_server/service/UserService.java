package com.amerbank.auth_server.service;

import com.amerbank.auth_server.exception.EmailAlreadyTakenException;
import com.amerbank.auth_server.exception.UserNotFoundException;
import com.amerbank.auth_server.model.User;
import com.amerbank.auth_server.repository.UserRepository;
import com.amerbank.common_dto.Role;
import com.amerbank.common_dto.UserRegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        user.setEmail(email);
        userRepository.save(user);
    }

    /**
     * Updates the password of a user.
     *
     * @param id the ID of the user to update
     * @param newPassword the new password
     * @throws UserNotFoundException if no user with the given ID exists
     */
    public void updatePassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
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
}
