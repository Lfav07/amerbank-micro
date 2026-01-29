package com.amerbank.account.controller;

import com.amerbank.account.dto.ErrorResponse;
import com.amerbank.account.dto.ValidationErrorResponse;
import com.amerbank.account.exception.*;
import com.amerbank.account.util.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class ApiExceptionHandler {

    private final TraceIdUtil traceIdUtil;

    // ============================================================
    // ================ 404 NOT_FOUND ============================
    // ============================================================

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(
            AccountNotFoundException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);
        
        log.warn("Account not found - TraceId: {}, Message: {}", traceId, ex.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Account Not Found")
                .message("Account not found")
                .path(path)
                .traceId(traceId)
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // ============================================================
    // ================ 409 CONFLICT =============================
    // ============================================================

    @ExceptionHandler(AccountAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleAccountAlreadyExists(
            AccountAlreadyExistsException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);
        
        log.warn("Account already exists - TraceId: {}, Message: {}", traceId, ex.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Conflict")
                .message("Account already exists")
                .path(path)
                .traceId(traceId)
                .build();
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // ============================================================
    // ================ 401 UNAUTHORIZED =========================
    // ============================================================

    @ExceptionHandler({
        UnauthorizedMicroserviceAccessException.class,
        AccessDeniedException.class
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
                .message("Unauthorized access")
                .path(path)
                .traceId(traceId)
                .build();
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // ============================================================
    // ================ 400 BAD_REQUEST =========================
    // ============================================================

    @ExceptionHandler({
        InactiveAccountException.class,
        InsufficientFundsAvailableException.class,
        NegativeRefundAmountException.class,
        SameRefundAccountsException.class,
        IllegalArgumentException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(
            RuntimeException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);
        
        log.warn("Bad request - TraceId: {}, Message: {}", traceId, ex.getMessage());
        
        String userMessage = getUserFriendlyMessage(ex);
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(userMessage)
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

    // ============================================================
    // ================ 503 SERVICE_UNAVAILABLE =================
    // ============================================================

    @ExceptionHandler(CustomerServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(
            CustomerServiceUnavailableException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);
        
        log.error("Customer service unavailable - TraceId: {}, Error: {}", traceId, ex.getMessage(), ex);
        
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
        AccountRegistrationFailedException.class,
        FailedToGenerateAccountNumberException.class,
        NullAccountBalanceException.class
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

    private String getUserFriendlyMessage(RuntimeException ex) {
        return switch (ex.getClass().getSimpleName()) {
            case "InactiveAccountException" -> "Account is not active";
            case "InsufficientFundsAvailableException" -> "Insufficient funds";
            case "NegativeRefundAmountException" -> "Refund amount must be positive";
            case "SameRefundAccountsException" -> "Source and destination accounts must be different";
            case "IllegalArgumentException" -> ex.getMessage();
            default -> "Invalid request";
        };
    }
}
