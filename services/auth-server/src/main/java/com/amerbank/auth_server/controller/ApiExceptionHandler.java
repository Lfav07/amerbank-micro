package com.amerbank.auth_server.controller;

import com.amerbank.auth_server.exception.EmailAlreadyTakenException;
import com.amerbank.auth_server.exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Map<String, String>> handleAuth(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
    }

    @ExceptionHandler(UserNotFoundException.class)
    ResponseEntity<Map<String, String>>handleUserNotFound(UserNotFoundException e) {
        return  ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
    }

    @ExceptionHandler(EmailAlreadyTakenException.class)
        ResponseEntity<Map<String, String>>handleEmailAlreadyTaken(EmailAlreadyTakenException e){
            return  ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Email already taken"));
        }


    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException e) {
        return  ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Incorrect username or password"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> handleArgumentNotValid(MethodArgumentNotValidException e) {
        return  ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Invalid argument"));
    }

}