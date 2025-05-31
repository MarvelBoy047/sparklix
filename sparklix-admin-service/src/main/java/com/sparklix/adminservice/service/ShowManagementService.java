package com.sparklix.adminservice.service;

import com.sparklix.adminservice.dto.CatalogShowDataDto;
import com.sparklix.adminservice.dto.ShowRequestDto;
import com.sparklix.adminservice.dto.ShowtimeRequestDto;
import com.sparklix.adminservice.dto.VenueRequestDto;
import com.sparklix.adminservice.entity.Show;
import com.sparklix.adminservice.entity.Showtime;
import com.sparklix.adminservice.entity.Venue;
import com.sparklix.adminservice.exception.ResourceConflictException; // Assuming this exists
import com.sparklix.adminservice.exception.ResourceNotFoundException; // Assuming this exists
import com.sparklix.adminservice.exception.DuplicateResourceException; // Create if needed
import com.sparklix.adminservice.repository.ShowRepository;
import com.sparklix.adminservice.repository.ShowtimeRepository;
import com.sparklix.adminservice.repository.VenueRepository;

import org.slf4j.Logger; // SLF4J Logger
import org.slf4j.LoggerFactory; // SLF4J Logger
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate; // For releaseDate comparison
// import java.time.LocalDateTime; // Not directly used here
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
// @Transactional // Apply @Transactional at method level for more fine-grained control if needed
public class ShowManagementService {

    private static final Logger logger = LoggerFactory.getLogger(ShowManagementService.class);

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

    // --- Venue Methods ---
    @Transactional
    public Venue createVenue(VenueRequestDto venueDto) {
        logger.debug("Attempting to create venue: Name='{}', City='{}'", venueDto.getName(), venueDto.getCity());
        if (venueRepository.existsByNameAndCity(venueDto.getName(), venueDto.getCity())) {
            String message = "Venue with name '" + venueDto.getName() + "' in city '" + venueDto.getCity() + "' already exists.";
            logger.warn(message);
            throw new DuplicateResourceException(message); // Or ResourceConflictException
        }
        Venue venue = new Venue();
        venue.setName(venueDto.getName());
        venue.setAddress(venueDto.getAddress());
        venue.setCity(venueDto.getCity());
        venue.setCapacity(venueDto.getCapacity());
        Venue savedVenue = venueRepository.save(venue);
        logger.info("Successfully created Venue ID: {}, Name: '{}'", savedVenue.getId(), savedVenue.getName());
        return savedVenue;
    }

    @Transactional(readOnly = true)
    public List<Venue> getAllVenues() {
        logger.debug("Fetching all venues.");
        return venueRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Venue getVenueById(Long id) {
        logger.debug("Fetching venue by ID: {}", id);
        return venueRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Venue not found with ID: {}", id);
                    return new ResourceNotFoundException("Venue", "id", id);
                });
    }

    @Transactional
    public Venue updateVenue(Long id, VenueRequestDto venueDto) {
        logger.debug("Attempting to update venue ID: {}", id);
        Venue venue = getVenueById(id); // Ensures venue exists, throws RNF if not

        // Check for duplicate name/city only if name or city is being changed to something that already exists for *another* venue
        if ((!venue.getName().equalsIgnoreCase(venueDto.getName()) || !venue.getCity().equalsIgnoreCase(venueDto.getCity())) &&
            venueRepository.existsByNameAndCityAndIdNot(venueDto.getName(), venueDto.getCity(), id)) {
            String message = "Another venue with name '" + venueDto.getName() + "' in city '" + venueDto.getCity() + "' already exists.";
            logger.warn(message);
            throw new DuplicateResourceException(message); // Or ResourceConflictException
        }

        venue.setName(venueDto.getName());
        venue.setAddress(venueDto.getAddress());
        venue.setCity(venueDto.getCity());
        venue.setCapacity(venueDto.getCapacity());
        Venue updatedVenue = venueRepository.save(venue);
        logger.info("Successfully updated Venue ID: {}, Name: '{}'", updatedVenue.getId(), updatedVenue.getName());
        return updatedVenue;
    }

    @Transactional
    public void deleteVenue(Long id) {
        logger.debug("Attempting to delete venue ID: {}", id);
        Venue venue = getVenueById(id); // Ensures venue exists, throws RNF if not
        if (showtimeRepository.existsByVenue(venue)) {
             String message = "Cannot delete venue ID " + id + ". It is currently associated with active showtimes.";
             logger.warn(message);
             throw new ResourceConflictException(message);
        }
        venueRepository.delete(venue); // Use delete(entity) for cascading if set up, or deleteById(id)
        logger.info("Successfully deleted Venue ID: {}", id);
    }

    // --- Show Methods ---
    @Transactional
    public Show createShow(ShowRequestDto showDto) {
        logger.debug("Attempting to create show: Title='{}'", showDto.getTitle());
        if (showRepository.existsByTitle(showDto.getTitle())) {
            String message = "Show with title '" + showDto.getTitle() + "' already exists.";
            logger.warn(message);
            throw new DuplicateResourceException(message); // Or ResourceConflictException
        }
        // Validate releaseDate (example - must be in future or present)
        if (showDto.getReleaseDate() != null && showDto.getReleaseDate().isBefore(LocalDate.now())) {
            String message = "Release date must be in the present or future.";
            logger.warn(message);
            // This should ideally be caught by @Valid if ShowRequestDto has @FutureOrPresent on releaseDate
            // If not, throw a business validation exception here, e.g., InvalidInputException or IllegalArgumentException
            throw new IllegalArgumentException(message);
        }

        Show show = new Show();
        show.setTitle(showDto.getTitle());
        show.setDescription(showDto.getDescription());
        show.setGenre(showDto.getGenre());
        show.setLanguage(showDto.getLanguage());
        show.setDurationMinutes(showDto.getDurationMinutes());
        show.setReleaseDate(showDto.getReleaseDate());
        show.setPosterUrl(showDto.getPosterUrl());
        Show savedShow = showRepository.save(show);
        logger.info("Successfully created Show ID: {}, Title: '{}'", savedShow.getId(), savedShow.getTitle());
        return savedShow;
    }

    @Transactional(readOnly = true)
    public List<Show> getAllShows() {
        logger.debug("Fetching all shows.");
        return showRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Show getShowById(Long id) {
        logger.debug("Fetching show by ID: {}", id);
        return showRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Show not found with ID: {}", id);
                    return new ResourceNotFoundException("Show", "id", id);
                });
    }

    @Transactional
    public Show updateShow(Long id, ShowRequestDto showDto) {
        logger.debug("Attempting to update show ID: {}", id);
        Show show = getShowById(id); // Ensures show exists

        if (!show.getTitle().equalsIgnoreCase(showDto.getTitle()) && 
            showRepository.existsByTitleAndIdNot(showDto.getTitle(), id)) {
            String message = "Another show with title '" + showDto.getTitle() + "' already exists.";
            logger.warn(message);
            throw new DuplicateResourceException(message); // Or ResourceConflictException
        }
        // Validate releaseDate for update as well
        if (showDto.getReleaseDate() != null && showDto.getReleaseDate().isBefore(LocalDate.now())) {
             String message = "Release date must be in the present or future.";
            logger.warn(message);
            throw new IllegalArgumentException(message);
        }

        show.setTitle(showDto.getTitle());
        show.setDescription(showDto.getDescription());
        show.setGenre(showDto.getGenre());
        show.setLanguage(showDto.getLanguage());
        show.setDurationMinutes(showDto.getDurationMinutes());
        show.setReleaseDate(showDto.getReleaseDate());
        show.setPosterUrl(showDto.getPosterUrl());
        Show updatedShow = showRepository.save(show);
        logger.info("Successfully updated Show ID: {}, Title: '{}'", updatedShow.getId(), updatedShow.getTitle());
        return updatedShow;
    }

    @Transactional
    public void deleteShow(Long id) {
        logger.debug("Attempting to delete show ID: {}", id);
        Show show = getShowById(id); // Ensures show exists
        if (showtimeRepository.existsByShow(show)) {
            String message = "Cannot delete show ID " + id + ". It is currently scheduled for showtimes.";
            logger.warn(message);
            throw new ResourceConflictException(message);
        }
        showRepository.delete(show);
        logger.info("Successfully deleted Show ID: {}", id);
    }

    // --- Showtime Methods ---
    @Transactional
    public Showtime createShowtime(ShowtimeRequestDto showtimeDto) {
        logger.debug("Attempting to create showtime for Show ID: {}, Venue ID: {}, DateTime: {}", 
                     showtimeDto.getShowId(), showtimeDto.getVenueId(), showtimeDto.getShowDateTime());
        Show show = getShowById(showtimeDto.getShowId());
        Venue venue = getVenueById(showtimeDto.getVenueId());

        if (showtimeRepository.existsByShowAndVenueAndShowDateTime(show, venue, showtimeDto.getShowDateTime())) {
            String message = "Showtime for show '" + show.getTitle() + "' at venue '" + venue.getName() + 
                             "' on " + showtimeDto.getShowDateTime() + " already exists.";
            logger.warn(message);
            throw new DuplicateResourceException(message); // Or ResourceConflictException
        }

        Showtime showtime = new Showtime();
        showtime.setShow(show);
        showtime.setVenue(venue);
        showtime.setShowDateTime(showtimeDto.getShowDateTime());
        showtime.setPricePerSeat(showtimeDto.getPricePerSeat());
        showtime.setTotalSeats(showtimeDto.getTotalSeats());
        Showtime savedShowtime = showtimeRepository.save(showtime);
        logger.info("Successfully created Showtime ID: {}, for Show: '{}', Venue: '{}'", 
                    savedShowtime.getId(), show.getTitle(), venue.getName());
        return savedShowtime;
    }

    @Transactional(readOnly = true)
    public List<Showtime> getAllShowtimes() {
        logger.debug("Fetching all showtimes.");
        return showtimeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Showtime getShowtimeById(Long id) {
        logger.debug("Fetching showtime by ID: {}", id);
        return showtimeRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Showtime not found with ID: {}", id);
                    return new ResourceNotFoundException("Showtime", "id", id);
                });
    }

    @Transactional(readOnly = true)
    public List<Showtime> getShowtimesByShowId(Long showId) {
        logger.debug("Fetching showtimes for Show ID: {}", showId);
        Show show = getShowById(showId); // Ensures show exists first
        return showtimeRepository.findByShow(show); // Use findByShow(Show entity)
    }

    @Transactional
    public Showtime updateShowtime(Long id, ShowtimeRequestDto showtimeDto) {
        logger.debug("Attempting to update showtime ID: {}", id);
        Showtime existingShowtime = getShowtimeById(id);

        Show show = getShowById(showtimeDto.getShowId());
        Venue venue = getVenueById(showtimeDto.getVenueId());

        boolean isKeyChanging = !Objects.equals(existingShowtime.getShow().getId(), show.getId()) ||
                                !Objects.equals(existingShowtime.getVenue().getId(), venue.getId()) ||
                                !existingShowtime.getShowDateTime().isEqual(showtimeDto.getShowDateTime());

        if (isKeyChanging && 
            showtimeRepository.existsByShowAndVenueAndShowDateTimeAndIdNot(show, venue, showtimeDto.getShowDateTime(), id)) {
            String message = "Another showtime for show '" + show.getTitle() + "' at venue '" + venue.getName() + 
                             "' on " + showtimeDto.getShowDateTime() + " already exists.";
            logger.warn(message);
            throw new DuplicateResourceException(message); // Or ResourceConflictException
        }

        existingShowtime.setShow(show);
        existingShowtime.setVenue(venue);
        existingShowtime.setShowDateTime(showtimeDto.getShowDateTime());
        existingShowtime.setPricePerSeat(showtimeDto.getPricePerSeat());
        existingShowtime.setTotalSeats(showtimeDto.getTotalSeats());
        Showtime updatedShowtime = showtimeRepository.save(existingShowtime);
        logger.info("Successfully updated Showtime ID: {}", updatedShowtime.getId());
        return updatedShowtime;
    }

    @Transactional
    public void deleteShowtime(Long id) {
        logger.debug("Attempting to delete showtime ID: {}", id);
        Showtime showtime = getShowtimeById(id); // Ensures it exists
        // TODO: Add business logic check: Cannot delete showtime if active bookings exist.
        // This would typically involve a call to booking-service or a shared flag/status.
        // For now, we proceed with direct deletion.
        // if (bookingServiceClient.hasActiveBookings(id)) {
        //    throw new ResourceConflictException("Cannot delete showtime. It has active bookings.");
        // }
        showtimeRepository.delete(showtime);
        logger.info("Successfully deleted Showtime ID: {}", id);
    }

    @Transactional(readOnly = true)
    public List<CatalogShowDataDto> getAllShowDataForCatalog() {
        logger.debug("Fetching all show data for catalog sync.");
        List<Showtime> allShowtimes = showtimeRepository.findAll();

        if (allShowtimes.isEmpty()) {
            logger.info("No showtimes found in admin-service to provide for catalog sync.");
            return new ArrayList<>();
        }
        logger.info("Found {} showtimes in admin-service to provide for catalog sync.", allShowtimes.size());
        return allShowtimes.stream()
            .map(st -> {
                // Basic null checks for safety, though data integrity should ensure these are present
                Show show = st.getShow();
                Venue venue = st.getVenue();
                if (show == null || venue == null) {
                    logger.warn("Skipping showtime ID {} for catalog sync due to missing show or venue reference.", st.getId());
                    return null; 
                }
                return new CatalogShowDataDto(
                    show.getId(), show.getTitle(), show.getDescription(),
                    show.getGenre(), show.getLanguage(), show.getDurationMinutes(),
                    show.getReleaseDate(), show.getPosterUrl(),
                    venue.getId(), venue.getName(), venue.getAddress(),
                    venue.getCity(), venue.getCapacity(),
                    st.getId(), st.getShowDateTime(), st.getPricePerSeat(), st.getTotalSeats()
                );
            })
            .filter(Objects::nonNull) // Filter out any nulls from problematic data
            .collect(Collectors.toList());
    }
}