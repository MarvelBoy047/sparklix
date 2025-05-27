package com.sparklix.adminservice.service;

import com.sparklix.adminservice.dto.CatalogShowDataDto;
import com.sparklix.adminservice.dto.ShowRequestDto;
import com.sparklix.adminservice.dto.ShowtimeRequestDto;
import com.sparklix.adminservice.dto.VenueRequestDto;
import com.sparklix.adminservice.entity.Show;
import com.sparklix.adminservice.entity.Showtime;
import com.sparklix.adminservice.entity.Venue;
import com.sparklix.adminservice.exception.ResourceConflictException;
import com.sparklix.adminservice.exception.ResourceNotFoundException;
import com.sparklix.adminservice.repository.ShowRepository;
import com.sparklix.adminservice.repository.ShowtimeRepository;
import com.sparklix.adminservice.repository.VenueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime; // Ensure this is imported
import java.util.ArrayList;
import java.util.List;
import java.util.Objects; // For checking ID changes in update
import java.util.stream.Collectors;

@Service
@Transactional
public class ShowManagementService {

    private final VenueRepository venueRepository;
    private final ShowRepository showRepository;
    private final ShowtimeRepository showtimeRepository;

    public ShowManagementService(VenueRepository venueRepository,
                                 ShowRepository showRepository,
                                 ShowtimeRepository showtimeRepository) {
        this.venueRepository = venueRepository;
        this.showRepository = showRepository;
        this.showtimeRepository = showtimeRepository;
    }

    // --- Venue Methods (Existing) ---
    public Venue createVenue(VenueRequestDto venueDto) {
        if (venueRepository.existsByNameAndCity(venueDto.getName(), venueDto.getCity())) {
            throw new ResourceConflictException("Venue with name '" + venueDto.getName() + "' in city '" + venueDto.getCity() + "' already exists.");
        }
        Venue venue = new Venue();
        venue.setName(venueDto.getName());
        venue.setAddress(venueDto.getAddress());
        venue.setCity(venueDto.getCity());
        venue.setCapacity(venueDto.getCapacity());
        return venueRepository.save(venue);
    }

    @Transactional(readOnly = true)
    public List<Venue> getAllVenues() {
        return venueRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Venue getVenueById(Long id) {
        return venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", id));
    }

    public Venue updateVenue(Long id, VenueRequestDto venueDto) {
        Venue venue = getVenueById(id);
        if (!venue.getName().equalsIgnoreCase(venueDto.getName()) || !venue.getCity().equalsIgnoreCase(venueDto.getCity())) {
            if (venueRepository.existsByNameAndCity(venueDto.getName(), venueDto.getCity())) {
                throw new ResourceConflictException("Another venue with name '" + venueDto.getName() + "' in city '" + venueDto.getCity() + "' already exists.");
            }
        }
        venue.setName(venueDto.getName());
        venue.setAddress(venueDto.getAddress());
        venue.setCity(venueDto.getCity());
        venue.setCapacity(venueDto.getCapacity());
        return venueRepository.save(venue);
    }

    public void deleteVenue(Long id) {
        Venue venue = getVenueById(id);
        // Basic check: if showtimes exist for this venue, prevent deletion
        if (showtimeRepository.existsByVenue(venue)) { // Assuming existsByVenue(Venue venue) method in ShowtimeRepository
             throw new ResourceConflictException("Cannot delete venue. It is currently used in showtimes.");
        }
        venueRepository.deleteById(id);
    }

    // --- Show Methods (Existing) ---
    public Show createShow(ShowRequestDto showDto) {
        if (showRepository.existsByTitle(showDto.getTitle())) {
            throw new ResourceConflictException("Show with title '" + showDto.getTitle() + "' already exists.");
        }
        Show show = new Show();
        show.setTitle(showDto.getTitle());
        show.setDescription(showDto.getDescription());
        show.setGenre(showDto.getGenre());
        show.setLanguage(showDto.getLanguage());
        show.setDurationMinutes(showDto.getDurationMinutes());
        show.setReleaseDate(showDto.getReleaseDate());
        show.setPosterUrl(showDto.getPosterUrl());
        return showRepository.save(show);
    }

    @Transactional(readOnly = true)
    public List<Show> getAllShows() {
        return showRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Show getShowById(Long id) {
        return showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show", "id", id));
    }

    public Show updateShow(Long id, ShowRequestDto showDto) {
        Show show = getShowById(id);
        if (!show.getTitle().equalsIgnoreCase(showDto.getTitle()) && showRepository.existsByTitle(showDto.getTitle())) {
            throw new ResourceConflictException("Another show with title '" + showDto.getTitle() + "' already exists.");
        }
        show.setTitle(showDto.getTitle());
        show.setDescription(showDto.getDescription());
        show.setGenre(showDto.getGenre());
        show.setLanguage(showDto.getLanguage());
        show.setDurationMinutes(showDto.getDurationMinutes());
        show.setReleaseDate(showDto.getReleaseDate());
        show.setPosterUrl(showDto.getPosterUrl());
        return showRepository.save(show);
    }

    public void deleteShow(Long id) {
        Show show = getShowById(id);
        // Basic check: if showtimes exist for this show, prevent deletion
        if (showtimeRepository.existsByShow(show)) { // Assuming existsByShow(Show show) method in ShowtimeRepository
            throw new ResourceConflictException("Cannot delete show. It is currently used in showtimes.");
        }
        showRepository.deleteById(id);
    }

    // --- Showtime Methods ---
    public Showtime createShowtime(ShowtimeRequestDto showtimeDto) {
        Show show = getShowById(showtimeDto.getShowId());
        Venue venue = getVenueById(showtimeDto.getVenueId());

        if (showtimeRepository.existsByShowAndVenueAndShowDateTime(show, venue, showtimeDto.getShowDateTime())) {
            throw new ResourceConflictException("Showtime for this show at this venue and time already exists.");
        }

        Showtime showtime = new Showtime();
        showtime.setShow(show);
        showtime.setVenue(venue);
        showtime.setShowDateTime(showtimeDto.getShowDateTime());
        showtime.setPricePerSeat(showtimeDto.getPricePerSeat());
        showtime.setTotalSeats(showtimeDto.getTotalSeats());
        return showtimeRepository.save(showtime);
    }

    @Transactional(readOnly = true)
    public List<Showtime> getAllShowtimes() {
        return showtimeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Showtime getShowtimeById(Long id) {
        return showtimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime", "id", id));
    }

    @Transactional(readOnly = true)
    public List<Showtime> getShowtimesByShowId(Long showId) {
        if (!showRepository.existsById(showId)) {
            throw new ResourceNotFoundException("Show", "id", showId);
        }
        return showtimeRepository.findByShowId(showId);
    }

    // NEW: Update Showtime Method
    public Showtime updateShowtime(Long id, ShowtimeRequestDto showtimeDto) {
        Showtime existingShowtime = getShowtimeById(id); // Ensures showtime exists

        Show show = getShowById(showtimeDto.getShowId());
        Venue venue = getVenueById(showtimeDto.getVenueId());

        // Check for conflict only if show, venue, or datetime is changing
        boolean isKeyChanging = !Objects.equals(existingShowtime.getShow().getId(), show.getId()) ||
                                !Objects.equals(existingShowtime.getVenue().getId(), venue.getId()) ||
                                !existingShowtime.getShowDateTime().isEqual(showtimeDto.getShowDateTime());

        if (isKeyChanging && showtimeRepository.existsByShowAndVenueAndShowDateTime(show, venue, showtimeDto.getShowDateTime())) {
            throw new ResourceConflictException("Another showtime for this show at this venue and time already exists.");
        }

        existingShowtime.setShow(show);
        existingShowtime.setVenue(venue);
        existingShowtime.setShowDateTime(showtimeDto.getShowDateTime());
        existingShowtime.setPricePerSeat(showtimeDto.getPricePerSeat());
        existingShowtime.setTotalSeats(showtimeDto.getTotalSeats());
        return showtimeRepository.save(existingShowtime);
    }

    // NEW: Delete Showtime Method
    public void deleteShowtime(Long id) {
        if (!showtimeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Showtime", "id", id);
        }
        // Add checks if bookings exist for this showtime before deleting, if applicable
        showtimeRepository.deleteById(id);
    }

    // --- Method for Internal Data Sync (Existing) ---
    @Transactional(readOnly = true)
    public List<CatalogShowDataDto> getAllShowDataForCatalog() {
        List<Showtime> allShowtimes = showtimeRepository.findAll();
        if (allShowtimes.isEmpty()) {
            return new ArrayList<>();
        }
        return allShowtimes.stream()
            .map(st -> new CatalogShowDataDto(
                st.getShow().getId(), st.getShow().getTitle(), st.getShow().getDescription(),
                st.getShow().getGenre(), st.getShow().getLanguage(), st.getShow().getDurationMinutes(),
                st.getShow().getReleaseDate(), st.getShow().getPosterUrl(),
                st.getVenue().getId(), st.getVenue().getName(), st.getVenue().getAddress(),
                st.getVenue().getCity(), st.getVenue().getCapacity(),
                st.getId(), st.getShowDateTime(), st.getPricePerSeat(), st.getTotalSeats()
            ))
            .collect(Collectors.toList());
    }
}