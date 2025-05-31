package com.sparklix.bookingservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequestDto {
    @NotNull(message = "Showtime ID is required.")
    private Long showtimeId;

    @NotNull(message = "Number of tickets is required.")
    @Min(value = 1, message = "At least one ticket must be booked.")
    private Integer numberOfTickets;
}