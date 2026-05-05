package com.ims.exception;

import com.ims.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

        // Incident Not Found
        @ExceptionHandler(IncidentNotFoundException.class)
        public ResponseEntity<ApiResponse<?>> handleIncidentNotFound(
                        IncidentNotFoundException ex) {
                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error(ex.getMessage()));
        }

        // Invalid State Transition
        @ExceptionHandler(InvalidStateTransitionException.class)
        public ResponseEntity<ApiResponse<?>> handleInvalidStateTransition(
                        InvalidStateTransitionException ex) {
                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(ex.getMessage()));
        }

        // RCA Incomplete (blocks CLOSE)
        @ExceptionHandler(RCAIncompleteException.class)
        public ResponseEntity<ApiResponse<?>> handleRCAIncomplete(
                        RCAIncompleteException ex) {
                return ResponseEntity
                                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                                .body(ApiResponse.error(ex.getMessage()));
        }

        // Rate Limit Exceeded
        @ExceptionHandler(RateLimitExceededException.class)
        public ResponseEntity<ApiResponse<?>> handleRateLimit(
                        RateLimitExceededException ex) {
                return ResponseEntity
                                .status(HttpStatus.TOO_MANY_REQUESTS)
                                .body(ApiResponse.error(ex.getMessage()));
        }

        // Validation Errors (@Valid)
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<?>> handleValidation(
                        MethodArgumentNotValidException ex) {
                String errors = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                                .collect(Collectors.joining(", "));
                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(errors));
        }

        // Generic Fallback
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<?>> handleGeneric(Exception ex) {
                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiResponse.error("Internal server error: "
                                                + ex.getMessage()));
        }
}