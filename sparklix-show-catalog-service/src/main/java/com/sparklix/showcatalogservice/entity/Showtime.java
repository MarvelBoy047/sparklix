package com.sparklix.showcatalogservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "showtimes_catalog")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // Good for LAZY loading issues with Jackson
public class Showtime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Local catalog ID for this showtime

    @Column(unique = true) // If one admin showtime maps to one catalog showtime
    private Long originalShowtimeId; // ID from the admin-service

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_show_id", nullable = false) // FK to shows_catalog table's 'id' column
    private Show show;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_venue_id", nullable = false) // FK to venues_catalog table's 'id' column
    private Venue venue;
    
    private LocalDateTime showDateTime;

    @Column(precision = 10, scale = 2) 
    private BigDecimal pricePerSeat;

    private int totalSeats; // This represents the total capacity for this showtime
    // If you need to track available seats, you'd add another field here like 'availableSeats'
    // and manage it during booking/cancellation.
}