package com.sparklix.showcatalogservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.util.List; // If you add a list of reviews or showtimes here

@Entity
@Table(name = "shows_catalog")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // ADDED
public class Show {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true) // originalShowId should be unique in this catalog table
    private Long originalShowId;

    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    private String genre;
    private String language;
    private int durationMinutes;
    private LocalDate releaseDate;
    private String posterUrl;

    // Example: If a Show has many Reviews (mappedBy "show" in Review entity)
    // @OneToMany(mappedBy = "show", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    // @JsonIgnoreProperties("show") // To prevent recursion if Review also has a Show back-reference
    // private List<Review> reviews;

    // Example: If a Show has many Showtimes (mappedBy "show" in Showtime entity)
    // @OneToMany(mappedBy = "show", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    // @JsonIgnoreProperties("show")
    // private List<Showtime> showtimes;
}