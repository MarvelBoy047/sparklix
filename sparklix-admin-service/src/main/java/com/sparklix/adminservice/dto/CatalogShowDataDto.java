package com.sparklix.adminservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatalogShowDataDto {
    // Show fields
    private Long showId;
    private String title;
    private String description;
    private String genre;
    private String language;
    private int durationMinutes;
    private LocalDate releaseDate;
    private String posterUrl;

    // Venue fields
    private Long venueId;
    private String venueName;
    private String venueAddress;
    private String venueCity;
    private int venueCapacity;

    // Showtime fields
    private Long showtimeId;
    private LocalDateTime showDateTime;
    private BigDecimal pricePerSeat;
    private int totalSeats;
}