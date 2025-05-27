package com.sparklix.showcatalogservice.controller;

import com.sparklix.showcatalogservice.dto.ReviewRequestDto;
import com.sparklix.showcatalogservice.dto.ReviewResponseDto;
import com.sparklix.showcatalogservice.dto.ShowtimeDetailsDto; // Ensure this is the correct DTO
import com.sparklix.showcatalogservice.entity.Show;
import com.sparklix.showcatalogservice.entity.Showtime;
import com.sparklix.showcatalogservice.entity.Venue; // Correctly imported
import com.sparklix.showcatalogservice.exception.ResourceNotFoundException; // Import if you throw it
import com.sparklix.showcatalogservice.repository.ShowRepository;
import com.sparklix.showcatalogservice.repository.ShowtimeRepository;
import com.sparklix.showcatalogservice.service.ReviewService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/shows")
public class ShowController {

    private final ShowRepository showRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ReviewService reviewService;

    public ShowController(ShowRepository showRepository,
                          ShowtimeRepository showtimeRepository,
                          ReviewService reviewService) {
        this.showRepository = showRepository;
        this.showtimeRepository = showtimeRepository;
        this.reviewService = reviewService;
    }

    // --- Show Endpoints ---
    @GetMapping
    public List<Show> getAllShows() {
        return showRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Show> getShowById(@PathVariable Long id) {
        Optional<Show> show = showRepository.findById(id);
        return show.map(ResponseEntity::ok)
                   .orElseThrow(() -> new ResourceNotFoundException("Show", "id", id)); // Throw exception
    }

    @GetMapping("/search")
    public List<Show> searchShowsByTitle(@RequestParam String title) {
        return showRepository.findByTitleContainingIgnoreCase(title);
    }

    @GetMapping("/genre/{genre}")
    public List<Show> getShowsByGenre(@PathVariable String genre) {
        return showRepository.findByGenre(genre);
    }

    // --- Showtime Endpoints related to a Show ---
    @GetMapping("/{showId}/showtimes")
    public ResponseEntity<List<Showtime>> getShowtimesForSpecificShow(@PathVariable Long showId) {
        if (!showRepository.existsById(showId)) {
            throw new ResourceNotFoundException("Show", "id", showId);
        }
        // CORRECTED: Use findByShow_Id as defined in ShowtimeRepository
        List<Showtime> showtimes = showtimeRepository.findByShow_Id(showId);
        return ResponseEntity.ok(showtimes);
    }

    @GetMapping("/showtimes/{showtimeId}/details-for-booking")
    public ResponseEntity<ShowtimeDetailsDto> getShowtimeDetailsForBooking(@PathVariable Long showtimeId) {
        Showtime st = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime", "id", showtimeId));

        Show show = st.getShow(); 
        Venue venue = st.getVenue(); 

        if (show == null || venue == null) {
             // This case should ideally not happen if data is consistent and FKs are in place
             // but good to handle defensively or log.
             throw new IllegalStateException("Show or Venue data is missing for Showtime ID: " + showtimeId);
        }

        // Ensure the DTO constructor arguments match the DTO definition
        // Assuming ShowtimeDetailsDto is: (Long originalShowtimeId, String showTitle, String venueName, 
        //                                   LocalDateTime showDateTime, BigDecimal pricePerSeat, int availableSeats)
        ShowtimeDetailsDto details = new ShowtimeDetailsDto(
                st.getOriginalShowtimeId(), // Or st.getId() if this is the primary key for the DTO
                show.getTitle(),
                venue.getName(),
                st.getShowDateTime(),
                st.getPricePerSeat(),
                st.getTotalSeats() // Using totalSeats as a proxy for availableSeats for now
                                   // Your ShowtimeDetailsDto should have a field named 'availableSeats'
                                   // OR you change the DTO to expect 'totalSeats'.
                                   // Let's assume the DTO expects 'availableSeats' and we pass totalSeats to it.
        );
        return ResponseEntity.ok(details);
    }

    // --- Review Endpoints related to a Show ---
    @PostMapping("/{showId}/reviews")
    @PreAuthorize("hasAnyRole('USER', 'VENDOR')")
    public ResponseEntity<ReviewResponseDto> addReviewToShow(@PathVariable Long showId,
                                                           @Valid @RequestBody ReviewRequestDto reviewRequestDto) {
        ReviewResponseDto createdReview = reviewService.addReview(showId, reviewRequestDto);
        return new ResponseEntity<>(createdReview, HttpStatus.CREATED);
    }

    @GetMapping("/{showId}/reviews")
    public ResponseEntity<List<ReviewResponseDto>> getReviewsForShow(@PathVariable Long showId) {
        List<ReviewResponseDto> reviews = reviewService.getReviewsForShow(showId);
        return ResponseEntity.ok(reviews);
    }
}