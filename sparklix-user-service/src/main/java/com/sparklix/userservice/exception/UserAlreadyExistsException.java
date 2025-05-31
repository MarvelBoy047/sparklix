package com.sparklix.userservice.exception;

// No specific @ResponseStatus here, let GlobalExceptionHandler handle it with 409
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}