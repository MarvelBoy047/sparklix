package com.sparklix.showcatalogservice.repository;

import com.sparklix.showcatalogservice.entity.Venue; // Uses the local Venue entity
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("catalogVenueRepository") // Optional: give it a distinct name if needed
public interface VenueRepository extends JpaRepository<Venue, Long> {
    // Find a local venue by the original ID from the admin service
    Optional<Venue> findByOriginalVenueId(Long originalVenueId);

    // You can add other query methods specific to catalog browsing needs later
    // e.g., List<Venue> findByCity(String city);
}