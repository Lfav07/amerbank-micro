package com.amerbank.transaction.controller;

import com.amerbank.transaction.dto.ErrorResponse;
import com.amerbank.transaction.dto.ValidationErrorResponse;
import com.amerbank.transaction.exception.*;
import com.amerbank.transaction.util.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import jakarta.validation.ConstraintViolationException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class ApiExceptionHandler {

    private final TraceIdUtil traceIdUtil;

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTransactionNotFound(
            TransactionNotFoundException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        log.warn("Transaction not found - TraceId: {}, Message: {}", traceId, ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Transaction Not Found")
                .message("Transaction not found")
                .path(path)
                .traceId(traceId)
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

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

    @ExceptionHandler(TransactionAlreadyRefundedException.class)
    public ResponseEntity<ErrorResponse> handleTransactionAlreadyRefunded(
            TransactionAlreadyRefundedException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        log.warn("Transaction already refunded - TraceId: {}, Message: {}", traceId, ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Conflict")
                .message("Transaction already refunded")
                .path(path)
                .traceId(traceId)
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler({
        UnauthorizedAccountAccessException.class,
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

    @ExceptionHandler({
        IdempotentRequestException.class,
        DepositFailedException.class,
        PaymentFailedException.class,
        RefundFailedException.class,
        IllegalArgumentException.class,
        MissingRequestHeaderException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(
            Exception ex,
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

    @ExceptionHandler(AccountServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(
            AccountServiceUnavailableException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        log.error("Account service unavailable - TraceId: {}, Error: {}", traceId, ex.getMessage(), ex);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Service Unavailable")
                .message("Account service unavailable")
                .path(path)
                .traceId(traceId)
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

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

    private String getTraceId(WebRequest request) {
        HttpServletRequest httpRequest = ((org.springframework.web.context.request.ServletWebRequest) request).getRequest();
        return traceIdUtil.extractOrGenerateTraceId(httpRequest);
    }

    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

    private String getUserFriendlyMessage(Exception ex) {
        return switch (ex.getClass().getSimpleName()) {
            case "IdempotentRequestException" -> "Invalid idempotency request";
            case "DepositFailedException" -> "Deposit failed";
            case "PaymentFailedException" -> "Payment failed";
            case "RefundFailedException" -> "Refund failed";
            case "MissingRequestHeaderException" -> "Required request header '" 
                    + ((MissingRequestHeaderException) ex).getHeaderName() + "' is not present";
            case "IllegalArgumentException" -> ex.getMessage();
            default -> "Invalid request";
        };
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

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
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
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            WebRequest request) {
        String traceId = getTraceId(request);
        String path = extractPath(request);

        String message = String.format("Invalid value for parameter '%s': '%s'",
                ex.getName(), ex.getValue());

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

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(
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
}
