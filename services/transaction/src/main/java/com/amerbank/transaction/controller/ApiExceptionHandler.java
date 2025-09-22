package com.amerbank.transaction.controller;

import com.amerbank.transaction.exception.UnauthorizedAccountAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(UnauthorizedAccountAccessException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorizedAccess(UnauthorizedAccountAccessException e) {
        return  ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized Access"));
    }
}
