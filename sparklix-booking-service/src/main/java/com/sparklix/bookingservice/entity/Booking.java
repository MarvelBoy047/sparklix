package com.sparklix.bookingservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId; // Username of the user who made the booking (from JWT)

    // These would be IDs from the show-catalog or admin service, stored here for reference
    // In a fully decoupled system, you might store more denormalized show/venue info too
    @Column(nullable = false)
    private Long originalShowtimeId; // The ID of the showtime from the source system

    // Denormalized data - good for booking history display without calling other services
    private String showTitle;
    private String venueName;
    private LocalDateTime showDateTime;

    @Column(nullable = false)
    private int numberOfTickets;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(nullable = false)
    private String bookingStatus; // e.g., PENDING_PAYMENT, CONFIRMED, CANCELLED

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}