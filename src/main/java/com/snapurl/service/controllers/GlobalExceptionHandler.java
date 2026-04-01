package com.snapurl.service.controllers;

import com.snapurl.service.dtos.ApiErrorResponse;
import com.snapurl.service.service.AccountLockedException;
import com.snapurl.service.service.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Bad request on path={} message={}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(IllegalStateException ex, HttpServletRequest request) {
        log.warn("Conflict on path={} message={}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccountLocked(AccountLockedException ex, HttpServletRequest request) {
        log.warn("Account locked response on path={} message={}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.LOCKED, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataConflict(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity conflict on path={}", request.getRequestURI(), ex);
        return buildResponse(HttpStatus.CONFLICT, "That email or username is already in use.", request.getRequestURI());
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ApiErrorResponse> handleDateParseError(DateTimeParseException ex, HttpServletRequest request) {
        log.warn("Date parse error on path={} message={}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid date format.", request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationError(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid request.")
                .orElse("Invalid request.");
        log.warn("Validation error on path={} message={}", request.getRequestURI(), message);
        return buildResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(UsernameNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found on path={} message={}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Unauthorized request on path={}", request.getRequestURI());
        return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid email or password.", request.getRequestURI());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimit(RateLimitExceededException ex, HttpServletRequest request) {
        log.warn("Rate limit exceeded on path={} limit={} retryAfter={}",
                request.getRequestURI(),
                ex.getRateLimitResult().getLimit(),
                ex.getRateLimitResult().getRetryAfterSeconds());
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("X-RateLimit-Limit", String.valueOf(ex.getRateLimitResult().getLimit()))
                .header("X-RateLimit-Remaining", String.valueOf(ex.getRateLimitResult().getRemaining()));
        if (ex.getRateLimitResult().getRetryAfterSeconds() > 0) {
            builder.header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRateLimitResult().getRetryAfterSeconds()));
        }
        return builder.body(new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected server error on path={}", request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error.", request.getRequestURI());
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String message, String path) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        ));
    }
}
