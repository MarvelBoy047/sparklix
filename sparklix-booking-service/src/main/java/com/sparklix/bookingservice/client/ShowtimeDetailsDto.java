package com.sparklix.bookingservice.client;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeDetailsDto {
    private Long originalShowtimeId;
    private String showTitle;
    private String venueName;
    private LocalDateTime showDateTime;
    private BigDecimal pricePerSeat;
    private int availableSeats;
}