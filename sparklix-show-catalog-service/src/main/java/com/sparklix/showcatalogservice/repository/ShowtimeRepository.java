package com.sparklix.showcatalogservice.repository;

import com.sparklix.showcatalogservice.entity.Showtime;
// No need to import Show or Venue here unless used in other custom query method signatures
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository("catalogShowtimeRepository")
public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

    Optional<Showtime> findByOriginalShowtimeId(Long originalShowtimeId);

    // Find local showtimes by the local catalog Show entity's ID
    List<Showtime> findByShow_Id(Long catalogShowId); // <<<--- CORRECTED METHOD NAME

    // Find local showtimes by the local catalog Venue entity's ID
    List<Showtime> findByVenue_Id(Long catalogVenueId); // <<<--- Also corrected for consistency

    @Query("SELECT st FROM Showtime st WHERE st.show.originalShowId = :originalShowId AND st.showDateTime >= :afterDateTime")
    List<Showtime> findByOriginalShowIdAndShowDateTimeAfter(
            @Param("originalShowId") Long originalShowId,
            @Param("afterDateTime") LocalDateTime afterDateTime
    );
}