package com.sparklix.bookingservice.client;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

 // This DTO's structure MUST match the response from the endpoint
 // in ShowCatalogService (e.g., /api/shows/showtimes/{showtimeId}/details-for-booking)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeDetailsDto {
     private Long originalShowtimeId; // The ID of the showtime in the catalog/admin system
     private String showTitle;
     private String venueName;
     private LocalDateTime showDateTime;
     private BigDecimal pricePerSeat;
     private int availableSeats;     // Crucial for checking if booking is possible
     // Add other fields if BookingService needs them from ShowCatalogService
}