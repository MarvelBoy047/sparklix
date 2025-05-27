package com.sparklix.adminservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ShowtimeRequestDto {
    @NotNull(message = "Show ID is required")
    private Long showId;

    @NotNull(message = "Venue ID is required")
    private Long venueId;

    @NotNull(message = "Show date and time is required")
    @Future(message = "Show date and time must be in the future")
    private LocalDateTime showDateTime;

    @NotNull(message = "Price per seat is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Price format is invalid")
    private BigDecimal pricePerSeat;

    @Min(value = 1, message = "Total seats must be at least 1")
    private int totalSeats;
}