package com.amerbank.account.controller;
import com.amerbank.account.exception.AccountNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public  ResponseEntity<Map<String, String>> handleAccountNotFound(AccountNotFoundException e) {
        return  ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "No accounts found for current user"));
    }
}
