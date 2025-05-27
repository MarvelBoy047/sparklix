package com.sparklix.adminservice.controller;

import com.sparklix.adminservice.dto.ShowRequestDto;
import com.sparklix.adminservice.dto.ShowtimeRequestDto;
import com.sparklix.adminservice.dto.VenueRequestDto;
import com.sparklix.adminservice.entity.Show;
import com.sparklix.adminservice.entity.Showtime;
import com.sparklix.adminservice.entity.Venue;
import com.sparklix.adminservice.service.ShowManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/management")
@PreAuthorize("hasRole('ADMIN')")
public class ShowManagementController {

    private final ShowManagementService showManagementService;

    public ShowManagementController(ShowManagementService showManagementService) {
        this.showManagementService = showManagementService;
    }

    @GetMapping("/hello")
    public String helloAdmin() {
        return "Hello from Sparklix Admin Service - Show Management!";
    }

    // --- Venue Endpoints (Existing) ---
    @PostMapping("/venues")
    public ResponseEntity<Venue> createVenue(@Valid @RequestBody VenueRequestDto venueDto) {
        Venue createdVenue = showManagementService.createVenue(venueDto);
        return new ResponseEntity<>(createdVenue, HttpStatus.CREATED);
    }

    @GetMapping("/venues")
    public List<Venue> getAllVenues() {
        return showManagementService.getAllVenues();
    }

    @GetMapping("/venues/{id}")
    public ResponseEntity<Venue> getVenueById(@PathVariable Long id) {
        return ResponseEntity.ok(showManagementService.getVenueById(id));
    }

    @PutMapping("/venues/{id}")
    public ResponseEntity<Venue> updateVenue(@PathVariable Long id, @Valid @RequestBody VenueRequestDto venueDto) {
        return ResponseEntity.ok(showManagementService.updateVenue(id, venueDto));
    }

    @DeleteMapping("/venues/{id}")
    public ResponseEntity<Void> deleteVenue(@PathVariable Long id) {
        showManagementService.deleteVenue(id);
        return ResponseEntity.noContent().build();
    }

    // --- Show Endpoints (Existing) ---
    @PostMapping("/shows")
    public ResponseEntity<Show> createShow(@Valid @RequestBody ShowRequestDto showDto) {
        Show createdShow = showManagementService.createShow(showDto);
        return new ResponseEntity<>(createdShow, HttpStatus.CREATED);
    }

    @GetMapping("/shows")
    public List<Show> getAllShows() {
        return showManagementService.getAllShows();
    }

    @GetMapping("/shows/{id}")
    public ResponseEntity<Show> getShowById(@PathVariable Long id) {
        return ResponseEntity.ok(showManagementService.getShowById(id));
    }

    @PutMapping("/shows/{id}")
    public ResponseEntity<Show> updateShow(@PathVariable Long id, @Valid @RequestBody ShowRequestDto showDto) {
        return ResponseEntity.ok(showManagementService.updateShow(id, showDto));
    }

    @DeleteMapping("/shows/{id}")
    public ResponseEntity<Void> deleteShow(@PathVariable Long id) {
        showManagementService.deleteShow(id);
        return ResponseEntity.noContent().build();
    }

    // --- Showtime Endpoints ---
    @PostMapping("/showtimes")
    public ResponseEntity<Showtime> createShowtime(@Valid @RequestBody ShowtimeRequestDto showtimeDto) {
        Showtime createdShowtime = showManagementService.createShowtime(showtimeDto);
        return new ResponseEntity<>(createdShowtime, HttpStatus.CREATED);
    }

    @GetMapping("/showtimes")
    public List<Showtime> getAllShowtimes() {
        return showManagementService.getAllShowtimes();
    }

    @GetMapping("/showtimes/{id}")
    public ResponseEntity<Showtime> getShowtimeById(@PathVariable Long id) {
        return ResponseEntity.ok(showManagementService.getShowtimeById(id));
    }

    @GetMapping("/shows/{showId}/showtimes") // Get showtimes for a specific show
    public List<Showtime> getShowtimesForShow(@PathVariable Long showId) {
        return showManagementService.getShowtimesByShowId(showId);
    }

    // NEW: Update Showtime Endpoint
    @PutMapping("/showtimes/{id}")
    public ResponseEntity<Showtime> updateShowtime(@PathVariable Long id, @Valid @RequestBody ShowtimeRequestDto showtimeDto) {
        Showtime updatedShowtime = showManagementService.updateShowtime(id, showtimeDto);
        return ResponseEntity.ok(updatedShowtime);
    }

    // NEW: Delete Showtime Endpoint
    @DeleteMapping("/showtimes/{id}")
    public ResponseEntity<Void> deleteShowtime(@PathVariable Long id) {
        showManagementService.deleteShowtime(id);
        return ResponseEntity.noContent().build();
    }
}