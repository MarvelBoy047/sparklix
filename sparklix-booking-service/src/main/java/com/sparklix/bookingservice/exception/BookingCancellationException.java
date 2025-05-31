package com.sparklix.bookingservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Could be a 400 Bad Request or 409 Conflict if the cancellation is not allowed
@ResponseStatus(HttpStatus.BAD_REQUEST) 
public class BookingCancellationException extends RuntimeException {
    public BookingCancellationException(String message) {
        super(message);
    }

    public BookingCancellationException(String message, Throwable cause) {
        super(message, cause);
    }
}