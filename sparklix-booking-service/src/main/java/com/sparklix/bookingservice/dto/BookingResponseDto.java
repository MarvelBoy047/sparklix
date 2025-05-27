package com.sparklix.bookingservice.dto;
import lombok.Data;
 import lombok.NoArgsConstructor;
 import lombok.AllArgsConstructor;
 import java.math.BigDecimal;
 import java.time.LocalDateTime;

 @Data
 @NoArgsConstructor
 @AllArgsConstructor
 public class BookingResponseDto {
     private Long bookingId;          // ID of this booking record
     private String userId;           // User who made the booking
     private Long originalShowtimeId; // ID of the showtime from the catalog/admin system
     private String showTitle;        // Denormalized for easy display
     private String venueName;        // Denormalized
     private LocalDateTime showDateTime;   // Denormalized
     private int numberOfTickets;
     private BigDecimal totalPrice;
     private String bookingStatus;    // e.g., "PENDING_PAYMENT", "CONFIRMED", "CANCELLED"
     private LocalDateTime createdAt;
 }