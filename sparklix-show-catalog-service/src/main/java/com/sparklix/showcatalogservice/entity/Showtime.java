package com.sparklix.showcatalogservice.entity;

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
public class Showtime {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private Long originalShowtimeId; // Store the ID from admin-service

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "catalog_show_id")
    private Show show; // Links to local catalog Show entity

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "catalog_venue_id")
    private Venue venue; // Links to local catalog Venue entity
    
    private LocalDateTime showDateTime;
    @Column(precision = 10, scale = 2) private BigDecimal pricePerSeat;
    private int totalSeats;
}