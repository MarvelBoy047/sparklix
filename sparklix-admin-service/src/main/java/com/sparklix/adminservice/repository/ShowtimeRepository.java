package com.sparklix.adminservice.repository;

import com.sparklix.adminservice.entity.Show;
import com.sparklix.adminservice.entity.Showtime;
import com.sparklix.adminservice.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // For custom query if needed for findAllWithDetails

import java.time.LocalDateTime;
import java.util.List;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
    boolean existsByShowAndVenueAndShowDateTime(Show show, Venue venue, LocalDateTime showDateTime);

    // Add these new methods:
    boolean existsByVenue(Venue venue);
    boolean existsByShow(Show show);
    List<Showtime> findByShow(Show show); // Changed from findByShowId to take Show entity

    // For checking duplicates during update, excluding self
    boolean existsByShowAndVenueAndShowDateTimeAndIdNot(Show show, Venue venue, LocalDateTime showDateTime, Long id);

    // For getAllShowDataForCatalog - to fetch details eagerly
    // Option 1: Using default findAll() and letting Hibernate handle fetching (might cause N+1)
    // No new method needed if you stick with just findAll() in the service.

    // Option 2: Using @EntityGraph (Recommended for performance)
    // @EntityGraph(attributePaths = {"show", "venue"}) // Eagerly fetch show and venue
    // List<Showtime> findAllWithDetails(); 
    // If you use this, change the service to call this method.
    // For now, I'll assume you'll use the simpler findAll() in the service,
    // and if performance becomes an issue, you can implement findAllWithDetails() with @EntityGraph.
    // So, the findAllWithDetails() method as a direct call is not strictly needed in the repo interface
    // if the service just uses findAll() and relies on Hibernate's fetching.
    // The service code I provided used `showtimeRepository.findAllWithDetails();` which was an assumption.
    // Let's simplify the service to use `findAll()` for now, and you can optimize later.
}