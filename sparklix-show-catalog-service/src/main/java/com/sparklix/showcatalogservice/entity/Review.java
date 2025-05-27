package com.sparklix.showcatalogservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews_catalog", indexes = { // Add index for faster lookups
    @Index(name = "idx_review_show_user", columnList = "catalog_show_id, userId", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_show_id", nullable = false) // Links to local Show entity
    private Show show;

    @Column(nullable = false)
    private String userId; // Username of the reviewer (from JWT)

    @Column(nullable = false)
    private int rating; // e.g., 1-5

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