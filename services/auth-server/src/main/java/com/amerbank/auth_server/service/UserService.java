package com.amerbank.auth_server.service;

import com.amerbank.auth_server.UserNotFoundException;
import com.amerbank.auth_server.model.User;
import com.amerbank.auth_server.repository.UserRepository;
import com.amerbank.auth_server.dto.UserRegisterRequest;
import com.amerbank.auth_server.dto.PasswordUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public boolean isEmailTaken(String email) {
        return userRepository.existsByEmailIgnoreCase(email.trim().toLowerCase());
    }

    public User registerUser(UserRegisterRequest request) {
        if (isEmailTaken(request.email())) {
            throw new IllegalArgumentException("Email already taken");
        }

        User user = User.builder()
                .email(request.email().trim().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .roles(request.roles())
                .active(true)
                .build();

        return userRepository.save(user);
    }

    public void updatePassword(PasswordUpdateRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean matches = passwordEncoder.matches(request.oldPassword(), user.getPassword());
        if (!matches) {
            throw new IllegalArgumentException("Old password does not match");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    public User findByEmail(String email) throws UserNotFoundException {
        return  userRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new UserNotFoundException("User not found!"));
    }
}
