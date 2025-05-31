package com.sparklix.bookingservice.exception;

import com.sparklix.bookingservice.dto.error.ErrorResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger G_LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        G_LOG.warn("BOOKING-SERVICE: Resource not found: {}. Path: {}", ex.getMessage(), path);
        ErrorResponseDto errorDetails = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                path,
                null // No validation errors for this type
        );
        return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        G_LOG.warn("BOOKING-SERVICE: Access Denied for path '{}': {}", path, ex.getMessage());
        ErrorResponseDto errorDetails = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                "You do not have permission to access this resource or perform this action.",
                path,
                null
        );
        return new ResponseEntity<>(errorDetails, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(InsufficientSeatsException.class)
    public ResponseEntity<ErrorResponseDto> handleInsufficientSeatsException(
            InsufficientSeatsException ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        G_LOG.warn("BOOKING-SERVICE: Insufficient seats / Booking conflict: {}. Path: {}", ex.getMessage(), path);
        ErrorResponseDto errorDetails = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "Booking Conflict",
                ex.getMessage(),
                path,
                null
        );
        return new ResponseEntity<>(errorDetails, HttpStatus.CONFLICT);
    }

    // ADD THIS HANDLER
    @ExceptionHandler(BookingCancellationException.class)
    public ResponseEntity<ErrorResponseDto> handleBookingCancellationException(
            BookingCancellationException ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        G_LOG.warn("BOOKING-SERVICE: Booking cancellation failed: {}. Path: {}", ex.getMessage(), path);
        ErrorResponseDto errorDetails = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(), // Or HttpStatus.CONFLICT if more appropriate
                "Cancellation Failed",
                ex.getMessage(),
                path,
                null
        );
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }


    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        G_LOG.warn("BOOKING-SERVICE: Validation error for path '{}': {}", path, ex.getBindingResult().getFieldErrors());
        List<String> errors = ex.getBindingResult().getFieldErrors()
                .stream().map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.toList());

        ErrorResponseDto errorDetails = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "Input validation failed. Please check your data.",
                path,
                errors);
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        G_LOG.warn("BOOKING-SERVICE: Illegal argument for path '{}': {}", path, ex.getMessage());
        ErrorResponseDto errorDetails = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Invalid Argument",
                ex.getMessage(),
                path,
                null
        );
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(RuntimeException.class) // Catch-all for other runtime exceptions
    public ResponseEntity<ErrorResponseDto> handleGenericRuntimeException(
            RuntimeException ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        G_LOG.error("BOOKING-SERVICE: Unexpected RuntimeException for path '{}':", path, ex);
        ErrorResponseDto errorDetails = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                path,
                null
        );
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class) // Most generic, should be last
    public ResponseEntity<ErrorResponseDto> handleAllUncaughtException(Exception ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        G_LOG.error("BOOKING-SERVICE: Critical Unhandled Exception for path '{}':", path, ex);
        ErrorResponseDto errorDetails = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "A critical server error occurred.",
                path,
                null);
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}