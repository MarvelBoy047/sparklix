package com.sparklix.bookingservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST) // Or a more specific status if applicable
public class BookingFailedException extends RuntimeException {
    public BookingFailedException(String message) {
        super(message);
    }
    public BookingFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}