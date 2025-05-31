package com.sparklix.showcatalogservice.controller;

import com.sparklix.showcatalogservice.dto.ShowtimeDetailsDto;
import com.sparklix.showcatalogservice.entity.Show;
import com.sparklix.showcatalogservice.entity.Showtime;
import com.sparklix.showcatalogservice.entity.Venue;
import com.sparklix.showcatalogservice.exception.ResourceNotFoundException;
import com.sparklix.showcatalogservice.repository.ShowRepository;
import com.sparklix.showcatalogservice.repository.ShowtimeRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/shows")
public class ShowController {

    private static final Logger logger = LoggerFactory.getLogger(ShowController.class);

    private final ShowRepository showRepository;
    private final ShowtimeRepository showtimeRepository;

    public ShowController(ShowRepository showRepository, ShowtimeRepository showtimeRepository) {
        this.showRepository = showRepository;
        this.showtimeRepository = showtimeRepository;
    }

    @GetMapping
    public List<Show> getAllShows() {
        logger.debug("Fetching all shows from catalog.");
        List<Show> shows = showRepository.findAll();
        logger.debug("Found {} shows in catalog.", shows.size());
        return shows;
    }

    @GetMapping("/{showCatalogId}")
    public ResponseEntity<Show> getShowById(@PathVariable("showCatalogId") Long showCatalogId) { // Explicitly named
        logger.debug("Fetching show by catalog ID: {}", showCatalogId);
        Show show = showRepository.findById(showCatalogId)
                .orElseThrow(() -> {
                    logger.warn("Show not found with catalog ID: {}", showCatalogId);
                    return new ResourceNotFoundException("Show", "id", showCatalogId);
                });
        return ResponseEntity.ok(show);
    }

    @GetMapping("/search")
    public List<Show> searchShowsByTitle(@RequestParam("title") String queryTitle) { // Explicit name is good
        logger.debug("Searching shows by title containing: {}", queryTitle);
        List<Show> shows = showRepository.findByTitleContainingIgnoreCase(queryTitle);
        logger.debug("Found {} shows matching title search for: {}", shows.size(), queryTitle);
        return shows;
    }

    @GetMapping("/genre/{genreName}")
    public List<Show> getShowsByGenre(@PathVariable("genreName") String genreName) { // Explicitly named
        logger.debug("Fetching shows by genre: {}", genreName);
        List<Show> shows = showRepository.findByGenre(genreName); // Assuming findByGenre exists
        logger.debug("Found {} shows for genre: {}", shows.size(), genreName);
        return shows;
    }

    @GetMapping("/{showCatalogId}/showtimes")
    public ResponseEntity<List<Showtime>> getShowtimesForSpecificShow(@PathVariable("showCatalogId") Long showCatalogId) { // Explicitly named
        logger.debug("Fetching showtimes for show with catalog ID: {}", showCatalogId);
        if (!showRepository.existsById(showCatalogId)) {
            logger.warn("Attempted to fetch showtimes for non-existent show with catalog ID: {}", showCatalogId);
            throw new ResourceNotFoundException("Show", "id", showCatalogId);
        }
        List<Showtime> showtimes = showtimeRepository.findByShow_Id(showCatalogId);
        logger.debug("Found {} showtimes for show catalog ID: {}", showtimes.size(), showCatalogId);
        return ResponseEntity.ok(showtimes != null ? showtimes : Collections.emptyList());
    }

    @GetMapping("/showtimes/{localShowtimeId}/details-for-booking")
    public ResponseEntity<ShowtimeDetailsDto> getShowtimeDetailsForBooking(@PathVariable("localShowtimeId") Long localShowtimeId) { // Explicitly named
        logger.debug("Fetching showtime details for booking for local showtime ID: {}", localShowtimeId);
        Showtime st = showtimeRepository.findById(localShowtimeId)
                .orElseThrow(() -> {
                    logger.warn("Showtime not found for booking details with local ID: {}", localShowtimeId);
                    return new ResourceNotFoundException("Showtime", "id", localShowtimeId);
                });

        Show show = st.getShow();
        Venue venue = st.getVenue();

        if (show == null) {
             logger.error("Data integrity issue: Show entity is null for Showtime with local ID: {}. OriginalShowtimeId: {}", localShowtimeId, st.getOriginalShowtimeId());
             throw new IllegalStateException("Show data is missing for Showtime ID: " + localShowtimeId + ". Associated show is null.");
        }
        if (venue == null) {
            logger.error("Data integrity issue: Venue entity is null for Showtime with local ID: {}. OriginalShowtimeId: {}", localShowtimeId, st.getOriginalShowtimeId());
            throw new IllegalStateException("Venue data is missing for Showtime ID: " + localShowtimeId + ". Associated venue is null.");
        }

        ShowtimeDetailsDto details = new ShowtimeDetailsDto(
                st.getOriginalShowtimeId(),
                show.getTitle(),
                venue.getName(),
                st.getShowDateTime(),
                st.getPricePerSeat(),
                st.getTotalSeats() 
        );
        logger.debug("Returning showtime details for booking: {}", details);
        return ResponseEntity.ok(details);
    }
}