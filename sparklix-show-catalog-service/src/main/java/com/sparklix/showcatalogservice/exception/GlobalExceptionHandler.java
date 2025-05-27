package com.sparklix.showcatalogservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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

    private static final Logger customLogger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        customLogger.warn("SHOW-CATALOG-SERVICE: Resource Not Found: {} (Details: {})", ex.getMessage(), request.getDescription(false));
        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), ex.getMessage(), request.getDescription(false).replace("uri=", ""), null);
        return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ErrorDetails> handleResourceConflictException(ResourceConflictException ex, WebRequest request) {
        customLogger.warn("SHOW-CATALOG-SERVICE: Resource Conflict: {} (Details: {})", ex.getMessage(), request.getDescription(false));
        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), ex.getMessage(), request.getDescription(false).replace("uri=", ""), null);
        return new ResponseEntity<>(errorDetails, HttpStatus.CONFLICT);
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorDetails> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        customLogger.warn("SHOW-CATALOG-SERVICE: Access Denied for path '{}': {}", path, ex.getMessage());
        ErrorDetails errorDetails = new ErrorDetails(
                LocalDateTime.now(),
                "Access Denied. You do not have the required permissions for this action.",
                ex.getMessage(), 
                null);
        return new ResponseEntity<>(errorDetails, HttpStatus.FORBIDDEN);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<String> validationErrors = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            validationErrors.add(error.getField() + ": " + error.getDefaultMessage());
        }
        for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            validationErrors.add(error.getObjectName() + ": " + error.getDefaultMessage());
        }
        customLogger.warn("SHOW-CATALOG-SERVICE: Validation Failed for path '{}': {}", request.getDescription(false).replace("uri=", ""), validationErrors);
        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), "Validation Failed", request.getDescription(false).replace("uri=", ""), validationErrors);
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class) // Generic fallback
    public ResponseEntity<ErrorDetails> handleGlobalException(Exception ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        customLogger.error("SHOW-CATALOG-SERVICE: Unhandled exception for path '{}': {}", path, ex.getMessage(), ex);
        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), "An unexpected internal server error occurred in the show catalog service.", ex.getClass().getName() + ": " + ex.getMessage(), null);
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}