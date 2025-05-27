package com.sparklix.bookingservice.service;

import com.sparklix.bookingservice.client.ShowtimeDetailsDto;
import com.sparklix.bookingservice.dto.BookingRequestDto;
import com.sparklix.bookingservice.dto.BookingResponseDto;
import com.sparklix.bookingservice.entity.Booking;
import com.sparklix.bookingservice.repository.BookingRepository;
// import com.sparklix.bookingservice.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired; // If using field injection
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {
    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final RestTemplate restTemplate; // Inject RestTemplate

    // @Autowired // Optional if only one constructor
    public BookingService(BookingRepository bookingRepository, RestTemplate restTemplate) {
        this.bookingRepository = bookingRepository;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public BookingResponseDto createBooking(BookingRequestDto bookingRequestDto) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String currentUserId = userDetails.getUsername();

        logger.info("User '{}' attempting to book {} tickets for showtime ID {} (using RestTemplate)",
                currentUserId, bookingRequestDto.getNumberOfTickets(), bookingRequestDto.getShowtimeId());

        // Step 1: Get Showtime details from Show Catalog Service using RestTemplate
        ShowtimeDetailsDto showtimeDetails;
        String showCatalogServiceUrl = "http://SHOW-CATALOG-SERVICE/api/shows/showtimes/" + bookingRequestDto.getShowtimeId() + "/details-for-booking";
        // Note: "SHOW-CATALOG-SERVICE" will be resolved by Eureka thanks to @LoadBalanced RestTemplate

        try {
            ResponseEntity<ShowtimeDetailsDto> responseEntity = restTemplate.getForEntity(showCatalogServiceUrl, ShowtimeDetailsDto.class);
            
            if (!responseEntity.getStatusCode().is2xxSuccessful() || responseEntity.getBody() == null) {
                logger.error("Failed to fetch showtime details for ID {}. Status: {}", bookingRequestDto.getShowtimeId(), responseEntity.getStatusCode());
                throw new RuntimeException("Showtime details not found or catalog service unavailable."); // TODO: Custom exception
            }
            showtimeDetails = responseEntity.getBody();
            logger.info("Successfully fetched showtime details: {}", showtimeDetails);

        } catch (HttpClientErrorException.NotFound e) {
             logger.warn("Showtime ID {} not found in catalog service.", bookingRequestDto.getShowtimeId());
             throw new RuntimeException("Specified showtime not found."); // TODO: Custom ResourceNotFoundException
        } catch (Exception e) { // Catch other RestClientException or general issues
            logger.error("Error calling show-catalog-service for showtime ID {}: {}", bookingRequestDto.getShowtimeId(), e.getMessage());
            throw new RuntimeException("Could not retrieve showtime information. Please try again later."); // TODO: Custom service unavailable exception
        }
        
        // Step 2: Validate availability (Simplified)
        if (showtimeDetails.getAvailableSeats() < bookingRequestDto.getNumberOfTickets()) {
            logger.warn("Not enough seats available for showtime ID {}. Requested: {}, Available: {}",
                    bookingRequestDto.getShowtimeId(), bookingRequestDto.getNumberOfTickets(), showtimeDetails.getAvailableSeats());
            throw new RuntimeException("Not enough seats available for this showtime."); // TODO: Custom InsufficientSeatsException
        }

        // Step 3: Calculate total price
        BigDecimal totalPrice = showtimeDetails.getPricePerSeat().multiply(BigDecimal.valueOf(bookingRequestDto.getNumberOfTickets()));

        // Step 4: Create and save the booking
        Booking booking = new Booking();
        booking.setUserId(currentUserId);
        booking.setOriginalShowtimeId(showtimeDetails.getOriginalShowtimeId());
        booking.setShowTitle(showtimeDetails.getShowTitle());
        booking.setVenueName(showtimeDetails.getVenueName());
        booking.setShowDateTime(showtimeDetails.getShowDateTime());
        booking.setNumberOfTickets(bookingRequestDto.getNumberOfTickets());
        booking.setTotalPrice(totalPrice);
        booking.setBookingStatus("PENDING_PAYMENT"); 
        // createdAt will be set by @PrePersist

        Booking savedBooking = bookingRepository.save(booking);
        logger.info("Booking created successfully for user '{}', booking ID: {} (using RestTemplate)", currentUserId, savedBooking.getId());

        return mapToBookingResponseDto(savedBooking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponseDto> getMyBookings() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String currentUserId = userDetails.getUsername();
        
        List<Booking> bookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(currentUserId);
        return bookings.stream().map(this::mapToBookingResponseDto).collect(Collectors.toList());
    }

    private BookingResponseDto mapToBookingResponseDto(Booking booking) {
        return new BookingResponseDto(
                booking.getId(),
                booking.getUserId(),
                booking.getOriginalShowtimeId(),
                booking.getShowTitle(),
                booking.getVenueName(),
                booking.getShowDateTime(),
                booking.getNumberOfTickets(),
                booking.getTotalPrice(),
                booking.getBookingStatus(),
                booking.getCreatedAt()
        );
    }
}