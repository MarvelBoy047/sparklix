package com.sparklix.showcatalogservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity 
@Table(name = "shows_catalog") 
@Data 
@NoArgsConstructor 
@AllArgsConstructor
public class Show {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private Long originalShowId; // Store the ID from admin-service
    private String title;
    @Lob 
    @Column(columnDefinition = "TEXT") private String description;
    private String genre;
    private String language;
    private int durationMinutes;
    private LocalDate releaseDate;
    private String posterUrl;
}