package com.sparklix.showcatalogservice.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// Using record for simplicity, ensure fields match admin-service's DTO
public record CatalogShowDataDto(
    Long showId, String title, String description, String genre, String language,
    int durationMinutes, LocalDate releaseDate, String posterUrl,
    Long venueId, String venueName, String venueAddress, String venueCity, int venueCapacity,
    Long showtimeId, LocalDateTime showDateTime, BigDecimal pricePerSeat, int totalSeats
) {}