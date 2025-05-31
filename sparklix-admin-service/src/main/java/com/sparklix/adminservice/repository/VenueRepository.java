package com.sparklix.adminservice.repository;

import com.sparklix.adminservice.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional; // If you don't have findByNameAndCity

public interface VenueRepository extends JpaRepository<Venue, Long> {
    boolean existsByNameAndCity(String name, String city);
    // Add this new method:
    boolean existsByNameAndCityAndIdNot(String name, String city, Long id);
    // Optional: If you want to find a venue by name and city for other purposes
    // Optional<Venue> findByNameAndCity(String name, String city);
}