package com.sparklix.adminservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@Entity
@Table(name = "shows")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Show {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String title;

    @Lob // For longer text
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String genre; // e.g., Movie, Play, Concert, Stand-up

    @Column(nullable = false)
    private String language;

    private int durationMinutes; // Duration in minutes

    private LocalDate releaseDate;

    private String posterUrl; // URL to the show's poster/image
    
    // Later we might add things like director, cast, ageRating etc.
}