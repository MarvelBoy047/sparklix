package com.sparklix.adminservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties; 

@Entity
@Table(name = "showtimes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Showtime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // A show can have many showtimes
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @ManyToOne(fetch = FetchType.LAZY) // A venue can host many showtimes
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @Column(nullable = false)
    private LocalDateTime showDateTime; // Date and time of the show

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerSeat;

    private int totalSeats; // Total seats available for this specific showtime
    // private int availableSeats; // We might manage this in booking-service or here if simple

    // We can add more details like screen_number if applicable for a venue later
}