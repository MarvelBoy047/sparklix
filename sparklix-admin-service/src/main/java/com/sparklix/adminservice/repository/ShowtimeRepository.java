package com.sparklix.adminservice.repository;

import com.sparklix.adminservice.entity.Show;
import com.sparklix.adminservice.entity.Showtime;
import com.sparklix.adminservice.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
    List<Showtime> findByShowId(Long showId);
    List<Showtime> findByVenueId(Long venueId); // Added for completeness, might be useful
    boolean existsByShowAndVenueAndShowDateTime(Show show, Venue venue, LocalDateTime showDateTime);

    // NEW methods for delete checks in service layer
    boolean existsByVenue(Venue venue);
    boolean existsByShow(Show show);
}