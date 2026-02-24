package com.amerbank.auth_server.controller;

import com.amerbank.auth_server.dto.ErrorResponse;
import com.amerbank.auth_server.dto.ValidationErrorResponse;
import com.amerbank.auth_server.exception.CustomerRegistrationFailedException;
import com.amerbank.auth_server.exception.CustomerServiceUnavailableException;
import com.amerbank.auth_server.exception.EmailAlreadyTakenException;
import com.amerbank.auth_server.exception.RegistrationFailedException;
import com.amerbank.auth_server.exception.UserNotFoundException;
import com.amerbank.auth_server.util.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class ApiExceptionHandler {
    private final TraceIdUtil traceIdUtil;

    // ============================================================
    // ================ 404 NOT_FOUND ============================
    // ============================================================

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserNotFoundException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        log.warn("User not found - TraceId: {}, Message: {}", traceId, ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("User Not Found")
                .message("User not found")
                .path(path)
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // ============================================================
    // ================ 409 CONFLICT =============================
    // ============================================================

    @ExceptionHandler(EmailAlreadyTakenException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyTaken(
            EmailAlreadyTakenException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        log.warn("Email already taken - TraceId: {}, Message: {}", traceId, ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Conflict")
                .message("Email already taken")
                .path(path)
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // ============================================================
    // ================ 401 UNAUTHORIZED =========================
    // ============================================================

    @ExceptionHandler({
        AuthenticationException.class,
        BadCredentialsException.class
    })
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            RuntimeException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        log.warn("Unauthorized access - TraceId: {}, Message: {}", traceId, ex.getMessage());

        String message = ex instanceof BadCredentialsException
                ? "Incorrect username or password"
                : "Unauthorized access";

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message(message)
                .path(path)
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // ============================================================
    // ================ 403 FORBIDDEN ===========================
    // ============================================================

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        log.warn("Access denied - TraceId: {}, Message: {}", traceId, ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message("Access denied")
                .path(path)
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // ============================================================
    // ================ 400 BAD_REQUEST =========================
    // ============================================================

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        log.warn("Data integrity violation - TraceId: {}, Message: {}", traceId, ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message("Data integrity violation")
                .path(path)
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        var fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (existing, replacement) -> existing
                ));

        log.warn("Validation failed - TraceId: {}, Errors: {}", traceId, fieldErrors);

        ValidationErrorResponse response = ValidationErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message("Validation failed")
                .errors(fieldErrors)
                .path(path)
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        log.warn("Malformed request body - TraceId: {}, Message: {}", traceId, ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message("Malformed request body")
                .path(path)
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        String message = String.format("Invalid value for parameter '%s'", ex.getName());
        log.warn("Type mismatch - TraceId: {}, Message: {}", traceId, message);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(message)
                .path(path)
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ============================================================
    // ================ 503 SERVICE_UNAVAILABLE =================
    // ============================================================

    @ExceptionHandler(CustomerServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleCustomerServiceUnavailable(
            CustomerServiceUnavailableException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        log.warn("Customer service unavailable - TraceId: {}, Message: {}", traceId, ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Service Unavailable")
                .message("Customer service unavailable")
                .path(path)
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    // ============================================================
    // ================ 500 INTERNAL_SERVER_ERROR ================
    // ============================================================

    @ExceptionHandler({
        RuntimeException.class
    })
    public ResponseEntity<ErrorResponse> handleInternalServerError(
            RuntimeException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        log.error("Internal server error - TraceId: {}, Error: {}", traceId, ex.getMessage(), ex);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Internal server error")
                .path(path)
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // ============================================================
    // ================ 500 REGISTRATION_FAILED ==================
    // ============================================================

    @ExceptionHandler(RegistrationFailedException.class)
    public ResponseEntity<ErrorResponse> handleRegistrationFailed(
            RegistrationFailedException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        log.error("Registration failed - TraceId: {}, Message: {}", traceId, ex.getMessage(), ex);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Registration failed")
                .path(path)
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // ============================================================
    // ================ 500 CUSTOMER_REGISTRATION_FAILED =========
    // ============================================================

    @ExceptionHandler(CustomerRegistrationFailedException.class)
    public ResponseEntity<ErrorResponse> handleCustomerRegistrationFailed(
            CustomerRegistrationFailedException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        log.error("Customer registration failed - TraceId: {}, Message: {}", traceId, ex.getMessage(), ex);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Failed to register customer")
                .path(path)
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // ============================================================
    // ================ 500 ILLEGAL_STATE =========================
    // ============================================================

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        log.error("Invalid response from customer service - TraceId: {}, Message: {}", traceId, ex.getMessage(), ex);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Invalid response from customer service")
                .path(path)
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // ============================================================
    // ================ GLOBAL FALLBACK ===========================
    // ============================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(
            Exception ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        log.error("Uncaught exception - TraceId: {}, Error: {}", traceId, ex.getMessage(), ex);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Internal server error")
                .path(path)
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // ============================================================
    // ================ PRIVATE HELPERS ===========================
    // ============================================================

    private String getTraceId(WebRequest request) {
        HttpServletRequest httpRequest = ((org.springframework.web.context.request.ServletWebRequest) request).getRequest();
        return traceIdUtil.extractOrGenerateTraceId(httpRequest);
    }

    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}