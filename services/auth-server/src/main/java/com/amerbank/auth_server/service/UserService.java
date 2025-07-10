package com.amerbank.auth_server.service;

import com.amerbank.auth_server.EmailAlreadyTakenException;
import com.amerbank.auth_server.UserNotFoundException;
import com.amerbank.auth_server.model.User;
import com.amerbank.auth_server.repository.UserRepository;

import com.amerbank.common_dto.UserRegisterRequest;
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
            throw new EmailAlreadyTakenException("Email already taken");
        }

        User user = User.builder()
                .email(request.email().trim().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .roles(request.roles())
                .active(true)
                .build();

        return userRepository.save(user);
    }

    public void updateEmail( Long id, String email)  {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        user.setEmail(email);
        userRepository.save(user);
    }

    public void updatePassword(Long id, String newPassword )  {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void deleteUser(Long id) throws UserNotFoundException {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found");
        }
        userRepository.deleteById(id);
    }

    public User findByEmail(String email){
        return userRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new UserNotFoundException("User not found!"));
    }
}
