package com.sparklix.adminservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
// Import AuthenticationException if you intend to handle it here,
// but JwtAuthenticationEntryPoint in admin-service should already cover most initial auth failures.
// import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger customLogger = LoggerFactory.getLogger(GlobalExceptionHandler.class); // Use a specific logger instance

    // Handle custom ResourceNotFoundException
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleResourceNotFoundException(ResourceNotFoundException ex,
                                                                        WebRequest request) {
        customLogger.warn("ADMIN-SERVICE: Resource Not Found: {} (Details: {})", ex.getMessage(), request.getDescription(false));
        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(),
                ex.getMessage(),
                request.getDescription(false).replace("uri=", ""), // Cleaner path
                null);
        return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
    }

    // Handle custom ResourceConflictException
    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ErrorDetails> handleResourceConflictException(ResourceConflictException ex,
                                                                        WebRequest request) {
        customLogger.warn("ADMIN-SERVICE: Resource Conflict: {} (Details: {})", ex.getMessage(), request.getDescription(false));
        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(),
                ex.getMessage(),
                request.getDescription(false).replace("uri=", ""),
                null);
        return new ResponseEntity<>(errorDetails, HttpStatus.CONFLICT);
    }

    // Handle Spring Security's AccessDeniedException
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorDetails> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        customLogger.warn("ADMIN-SERVICE: Access Denied for path '{}': {}", path, ex.getMessage());
        ErrorDetails errorDetails = new ErrorDetails(
                LocalDateTime.now(),
                "Access Denied. You do not have the required role/permission.", // User-friendly message
                ex.getMessage(), // Specific system message for detail/logging
                null
        );
        return new ResponseEntity<>(errorDetails, HttpStatus.FORBIDDEN);
    }

    // Override to handle @Valid DTO validation errors for admin-service
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<String> validationErrors = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            validationErrors.add(error.getField() + ": " + error.getDefaultMessage());
        }
        for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            validationErrors.add(error.getObjectName() + ": " + error.getDefaultMessage());
        }
        customLogger.warn("ADMIN-SERVICE: Validation Failed for path '{}': {}", request.getDescription(false).replace("uri=", ""), validationErrors);
        ErrorDetails errorDetails = new ErrorDetails(
                LocalDateTime.now(),
                "Validation Failed",
                request.getDescription(false).replace("uri=", ""),
                validationErrors);
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    // Generic fallback handler for any other unhandled exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleGlobalException(Exception ex,
                                                              WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        customLogger.error("ADMIN-SERVICE: Unhandled exception for path '{}': {}", path, ex.getMessage(), ex); // Log full exception
        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(),
                "An unexpected internal server error occurred in the admin service.",
                ex.getClass().getName() + ": " + ex.getMessage(), // More specific detail
                null);
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}