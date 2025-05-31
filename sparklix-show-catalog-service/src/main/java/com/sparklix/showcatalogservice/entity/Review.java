package com.sparklix.showcatalogservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // ADDED
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews_catalog", indexes = {
    @Index(name = "idx_review_show_user", columnList = "catalog_show_id, userId", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // ADDED
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // Keep LAZY
    @JoinColumn(name = "catalog_show_id", nullable = false)
    private Show show;

    @Column(nullable = false)
    private String userId; 

    @Column(nullable = false)
    private int rating; 

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String comment;

    @Column(nullable = false, updatable = false)
    private LocalDateTime reviewDate;

    @PrePersist
    protected void onCreate() {
        reviewDate = LocalDateTime.now();
    }
}