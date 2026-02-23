package com.amerbank.customer.customer.controller;

import com.amerbank.customer.customer.dto.ErrorResponse;
import com.amerbank.customer.customer.dto.ValidationErrorResponse;
import com.amerbank.customer.customer.exception.AuthServiceUnavailableException;
import com.amerbank.customer.customer.exception.CustomerAlreadyExistsException;
import com.amerbank.customer.customer.exception.CustomerNotFoundException;
import com.amerbank.customer.customer.exception.CustomerRegistrationFailedException;
import com.amerbank.customer.customer.exception.EmailAlreadyTakenException;
import com.amerbank.customer.customer.exception.InvalidCredentialsException;
import com.amerbank.customer.customer.exception.InvalidUserDataException;
import com.amerbank.customer.customer.exception.ServiceAuthenticationException;
import com.amerbank.customer.customer.exception.ServiceAuthorizationException;
import com.amerbank.customer.customer.exception.UserRegistrationFailedException;
import com.amerbank.customer.customer.util.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
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
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class ApiExceptionHandler {

    private final TraceIdUtil traceIdUtil;

    // ============================================================
    // ================ 404 NOT_FOUND ============================
    // ============================================================

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCustomerNotFound(
            CustomerNotFoundException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        log.warn("Customer not found - TraceId: {}, Message: {}", traceId, ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Customer Not Found")
                .message("Customer not found")
                .path(path)
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElement(
            NoSuchElementException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        log.warn("Resource not found - TraceId: {}, Message: {}", traceId, ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message("Resource not found")
                .path(path)
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // ============================================================
    // ================ 409 CONFLICT =============================
    // ============================================================

    @ExceptionHandler(CustomerAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleCustomerAlreadyExists(
            CustomerAlreadyExistsException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        log.warn("Customer already exists - TraceId: {}, Message: {}", traceId, ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Conflict")
                .message("Customer already exists")
                .path(path)
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

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
        InvalidCredentialsException.class,
        AuthenticationException.class,
        BadCredentialsException.class,
        ServiceAuthenticationException.class
    })
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            RuntimeException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        log.warn("Unauthorized access - TraceId: {}, Message: {}", traceId, ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message("Invalid credentials")
                .path(path)
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // ============================================================
    // ================ 403 FORBIDDEN ===========================
    // ============================================================

    @ExceptionHandler({
        AccessDeniedException.class,
        ServiceAuthorizationException.class
    })
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            RuntimeException ex,
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

    @ExceptionHandler(InvalidUserDataException.class)
    public ResponseEntity<ErrorResponse> handleInvalidUserData(
            InvalidUserDataException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        log.warn("Invalid user data - TraceId: {}, Message: {}", traceId, ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message("Invalid user data")
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

    @ExceptionHandler({
        IllegalArgumentException.class,
        IllegalStateException.class
    })
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            RuntimeException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        log.warn("Invalid request - TraceId: {}, Message: {}", traceId, ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .path(path)
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
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

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        log.warn("Missing request parameter - TraceId: {}, Parameter: {}", traceId, ex.getParameterName());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message("Missing required parameter: " + ex.getParameterName())
                .path(path)
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        log.warn("Type mismatch - TraceId: {}, Parameter: {}, Expected: {}",
                traceId, ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message("Invalid value for parameter: " + ex.getName())
                .path(path)
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        var violations = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        v -> v.getMessage(),
                        (existing, replacement) -> existing
                ));

        log.warn("Constraint violation - TraceId: {}, Violations: {}", traceId, violations);

        ValidationErrorResponse response = ValidationErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message("Validation failed")
                .errors(violations)
                .path(path)
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ============================================================
    // ================ 503 SERVICE_UNAVAILABLE =================
    // ============================================================

    @ExceptionHandler(AuthServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(
            AuthServiceUnavailableException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        log.error("Authentication service is temporarily unavailable - TraceId: {}, Error: {}", traceId, ex.getMessage(), ex);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Service Unavailable")
                .message("Authentication service is temporarily unavailable")
                .path(path)
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    // ============================================================
    // ================ 500 INTERNAL_SERVER_ERROR ================
    // ============================================================

    @ExceptionHandler({
        CustomerRegistrationFailedException.class,
        UserRegistrationFailedException.class
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
