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
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.servlet.NoHandlerFoundException;

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
// ================ 403 FORBIDDEN ==============================
// ============================================================

    @ExceptionHandler(AccountOwnershipException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(
            AccountOwnershipException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        log.warn("Forbidden operation - TraceId: {}, Message: {}", traceId, ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message("You are not authorized to perform this operation")
                .path(path)
                .traceId(traceId)
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
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
    // ================ 400 BAD_REQUEST (Other) ==================
    // ============================================================

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
                .message("Malformed request body or invalid JSON")
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

    // ============================================================
    // ================ 404 NOT_FOUND (No Handler) ==============
    // ============================================================

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        log.warn("No handler found - TraceId: {}, Path: {}", traceId, ex.getRequestURL());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message("Endpoint not found")
                .path(path)
                .traceId(traceId)
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // ============================================================
    // ================ 405 METHOD_NOT_ALLOWED ===================
    // ============================================================

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        log.warn("Method not supported - TraceId: {}, Method: {}, Path: {}",
                traceId, ex.getMethod(), ex.getSupportedHttpMethods());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .error("Method Not Allowed")
                .message("HTTP method " + ex.getMethod() + " not supported for this endpoint")
                .path(path)
                .traceId(traceId)
                .build();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    // ============================================================
    // ================ 415 UNSUPPORTED_MEDIA_TYPE ===============
    // ============================================================

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        log.warn("Media type not supported - TraceId: {}, ContentType: {}", traceId, ex.getContentType());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
                .error("Unsupported Media Type")
                .message("Content type " + ex.getContentType() + " not supported")
                .path(path)
                .traceId(traceId)
                .build();

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(response);
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
