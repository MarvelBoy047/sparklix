package com.sparklix.bookingservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ShowtimeNotFoundException extends RuntimeException {
    public ShowtimeNotFoundException(String message) {
        super(message);
    }
    public ShowtimeNotFoundException(Long showtimeId) {
        super("Showtime details not found for ID: " + showtimeId);
    }
}