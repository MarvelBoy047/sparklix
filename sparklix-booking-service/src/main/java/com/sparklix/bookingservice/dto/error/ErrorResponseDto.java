package com.sparklix.bookingservice.dto.error;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map; // Added for potential future use

// Using Lombok for getters, setters, constructors for brevity
// If not using Lombok, add them manually as in your example
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Includes @Getter, @Setter, @ToString, @EqualsAndHashCode, @RequiredArgsConstructor
@NoArgsConstructor // For default constructor
public class ErrorResponseDto {
    private LocalDateTime timestamp;
    private int status;
    private String error; 
    private String message; 
    private String path;
    private List<String> validationErrors; 
    private Map<String, Object> details;   

    public ErrorResponseDto(LocalDateTime timestamp, int status, String error, String message, String path) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    public ErrorResponseDto(LocalDateTime timestamp, int status, String error, String message, String path, List<String> validationErrors) {
        this(timestamp, status, error, message, path);
        this.validationErrors = validationErrors;
    }

    public ErrorResponseDto(LocalDateTime timestamp, int status, String error, String message, String path, List<String> validationErrors, Map<String, Object> details) {
        this(timestamp, status, error, message, path, validationErrors);
        this.details = details;
    }
}