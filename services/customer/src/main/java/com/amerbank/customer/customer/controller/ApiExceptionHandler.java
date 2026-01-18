package com.amerbank.customer.customer.controller;



import com.amerbank.customer.customer.exception.AuthServiceUnavailableException;
import com.amerbank.customer.customer.exception.CustomerAlreadyExistsException;
import com.amerbank.customer.customer.exception.CustomerNotFoundException;
import com.amerbank.customer.customer.exception.CustomerRegistrationFailedException;
import com.amerbank.customer.customer.exception.InvalidCredentialsException;
import com.amerbank.customer.customer.exception.UserRegistrationFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<String> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }

    @ExceptionHandler(AuthServiceUnavailableException.class)
    public ResponseEntity<String> handleServiceUnavailable(AuthServiceUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Authentication service unavailable");
    }

    @ExceptionHandler(CustomerAlreadyExistsException.class)
    public ResponseEntity<String> handleCustomerAlreadyExists(CustomerAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Customer already exists");
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<String> handleCustomerNotFound(CustomerNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Customer not found");
    }

    @ExceptionHandler(CustomerRegistrationFailedException.class)
    public ResponseEntity<String> handleCustomerRegistrationFailed(CustomerRegistrationFailedException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Customer registration failed");
    }

    @ExceptionHandler(UserRegistrationFailedException.class)
    public ResponseEntity<String> handleUserRegistrationFailed(UserRegistrationFailedException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("User registration failed");
    }
}
