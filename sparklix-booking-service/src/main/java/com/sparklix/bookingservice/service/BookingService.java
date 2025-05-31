package com.sparklix.bookingservice.service;

import com.sparklix.bookingservice.client.RazorpayOrderRequestDto; // NEW IMPORT
import com.sparklix.bookingservice.client.RazorpayOrderResponseDto; // NEW IMPORT
import com.sparklix.bookingservice.client.ShowtimeDetailsDto;
import com.sparklix.bookingservice.dto.BookingRequestDto;
import com.sparklix.bookingservice.dto.BookingResponseDto;
import com.sparklix.bookingservice.entity.Booking;
import com.sparklix.bookingservice.exception.BookingCancellationException;
import com.sparklix.bookingservice.repository.BookingRepository;
import com.sparklix.bookingservice.exception.ResourceNotFoundException;
import com.sparklix.bookingservice.exception.InsufficientSeatsException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID; // NEW IMPORT for receipt ID
import java.util.stream.Collectors;
import com.sparklix.bookingservice.client.RazorpayOrderRequestDto;
import com.sparklix.bookingservice.client.RazorpayOrderResponseDto;
import org.springframework.http.HttpEntity; // For requestEntity if needed
import org.springframework.http.HttpHeaders; // For requestEntity if needed
import org.springframework.http.MediaType;   // For requestEntity if needed
import java.util.UUID;

@Service
public class BookingService {
    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final RestTemplate loadBalancedRestTemplate;

    private static final String STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_PAYMENT_FAILED = "PAYMENT_FAILED";
    private static final String STATUS_PAYMENT_INIT_FAILED = "PAYMENT_INIT_FAILED";
    private static final List<String> CANCELLABLE_STATUSES = Arrays.asList(STATUS_PENDING_PAYMENT, STATUS_CONFIRMED);

    public BookingService(BookingRepository bookingRepository,
                          @Qualifier("loadBalancedRestTemplate") RestTemplate loadBalancedRestTemplate) {
        this.bookingRepository = bookingRepository;
        this.loadBalancedRestTemplate = loadBalancedRestTemplate;
    }

    @Transactional
    public BookingResponseDto createBooking(BookingRequestDto bookingRequestDto) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String currentUserId = userDetails.getUsername();

        logger.info("BOOKING-SERVICE: User '{}' attempting to book {} tickets for showtime ID (catalog local): {}",
                currentUserId, bookingRequestDto.getNumberOfTickets(), bookingRequestDto.getShowtimeId());

        ShowtimeDetailsDto showtimeDetails = fetchShowtimeDetails(bookingRequestDto.getShowtimeId());

        if (showtimeDetails.getAvailableSeats() < bookingRequestDto.getNumberOfTickets()) {
            String errorMessage = String.format(
                "Not enough seats available for showtime (Admin Original ID: %d, Title: %s). Requested: %d, Available: %d",
                showtimeDetails.getOriginalShowtimeId(),
                showtimeDetails.getShowTitle(),
                bookingRequestDto.getNumberOfTickets(),
                showtimeDetails.getAvailableSeats()
            );
            logger.warn("BOOKING-SERVICE: " + errorMessage);
            throw new InsufficientSeatsException(errorMessage);
        }

        BigDecimal totalPrice = showtimeDetails.getPricePerSeat()
                                .multiply(BigDecimal.valueOf(bookingRequestDto.getNumberOfTickets()));

        Booking booking = new Booking();
        booking.setUserId(currentUserId);
        if (showtimeDetails.getOriginalShowtimeId() == null) {
            logger.error("BOOKING-SERVICE: Critical data missing - OriginalShowtimeId for catalog showtimeId {}", bookingRequestDto.getShowtimeId());
            throw new IllegalStateException("Critical data (OriginalShowtimeId) missing from showtime details.");
        }
        booking.setOriginalShowtimeId(showtimeDetails.getOriginalShowtimeId());
        booking.setShowTitle(showtimeDetails.getShowTitle());
        booking.setVenueName(showtimeDetails.getVenueName());
        booking.setShowDateTime(showtimeDetails.getShowDateTime());
        booking.setNumberOfTickets(bookingRequestDto.getNumberOfTickets());
        booking.setTotalPrice(totalPrice);
        booking.setBookingStatus(STATUS_PENDING_PAYMENT);

        Booking savedBooking = bookingRepository.save(booking);
        logger.info("BOOKING-SERVICE: Booking ID {} created with PENDING_PAYMENT for user '{}'", savedBooking.getId(), currentUserId);

        initiatePayment(savedBooking); // Call initiate payment

        return mapToBookingResponseDto(savedBooking);
    }

    private ShowtimeDetailsDto fetchShowtimeDetails(Long catalogShowtimeId) {
        String showCatalogServiceUrl = "http://SHOW-CATALOG-SERVICE/api/shows/showtimes/" +
                                        catalogShowtimeId + "/details-for-booking";
        try {
            logger.debug("BOOKING-SERVICE: Calling Show Catalog Service: {}", showCatalogServiceUrl);
            ResponseEntity<ShowtimeDetailsDto> responseEntity = loadBalancedRestTemplate.exchange(
                showCatalogServiceUrl, HttpMethod.GET, null, ShowtimeDetailsDto.class
            );
            ShowtimeDetailsDto showtimeDetails = responseEntity.getBody();
            if (showtimeDetails == null) {
                logger.error("BOOKING-SERVICE: Showtime details body is null from Show Catalog Service for showtime ID (catalog local): {}.", catalogShowtimeId);
                throw new RuntimeException("Failed to retrieve valid showtime details (null body from catalog service).");
            }
            logger.debug("BOOKING-SERVICE: Received showtime details: {}", showtimeDetails);
            return showtimeDetails;
        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("BOOKING-SERVICE: Showtime (catalog local ID: {}) not found in Show Catalog Service. Status: {}. Body: {}",
                         catalogShowtimeId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new ResourceNotFoundException("Showtime (from catalog)", "id", catalogShowtimeId);
        } catch (RestClientException e) {
            logger.error("BOOKING-SERVICE: Error communicating with Show Catalog Service for showtime ID {}: {}",
                         catalogShowtimeId, e.getMessage(), e);
            throw new RuntimeException("Could not retrieve showtime information from Show Catalog Service. Please try again later.", e);
        }
    }

    private void initiatePayment(Booking booking) {
        try {
            RazorpayOrderRequestDto orderRequest = new RazorpayOrderRequestDto(
                    booking.getId(),
                    booking.getTotalPrice(),
                    "INR", 
                    "receipt_booking_" + booking.getId() + "_" + UUID.randomUUID().toString().substring(0, 8),
                    booking.getUserId()
            );

            String paymentServiceUrl = "http://PAYMENT-SERVICE/api/payments/create-order";
            logger.info("BOOKING-SERVICE: Calling Payment Service to create Razorpay order: {} for bookingId {}", paymentServiceUrl, booking.getId());
            logger.debug("BOOKING-SERVICE: Payment Order Request Payload: {}", orderRequest);

            // Since payment-service create-order is now permitAll, no Auth header needed for this call
            HttpEntity<RazorpayOrderRequestDto> requestEntity = new HttpEntity<>(orderRequest);

            ResponseEntity<RazorpayOrderResponseDto> paymentOrderResponse = loadBalancedRestTemplate.postForEntity(
                    paymentServiceUrl, requestEntity, RazorpayOrderResponseDto.class);

            if (paymentOrderResponse.getStatusCode() == HttpStatus.OK && paymentOrderResponse.getBody() != null) {
                RazorpayOrderResponseDto razorpayOrder = paymentOrderResponse.getBody();
                logger.info("BOOKING-SERVICE: Razorpay order created by payment-service. Razorpay Order ID: {}, Internal Booking ID: {}",
                            razorpayOrder.getRazorpayOrderId(), razorpayOrder.getInternalBookingId());
                // The booking status remains PENDING_PAYMENT. It will be updated by webhook or verify flow.
            } else {
                logger.error("BOOKING-SERVICE: Failed to create Razorpay order via payment-service for bookingId {}. Status: {}, Body: {}",
                        booking.getId(), paymentOrderResponse.getStatusCode(), paymentOrderResponse.getBody());
                booking.setBookingStatus(STATUS_PAYMENT_INIT_FAILED);
                bookingRepository.save(booking);
            }
        } catch (RestClientException e) {
            logger.error("BOOKING-SERVICE: Error calling payment-service to create Razorpay order for bookingId {}: {}",
                         booking.getId(), e.getMessage(), e);
            booking.setBookingStatus(STATUS_PAYMENT_INIT_FAILED);
            bookingRepository.save(booking);
        }
    }

    @Transactional
    public BookingResponseDto cancelBooking(Long bookingId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        logger.info("BOOKING-SERVICE: User '{}' attempting to cancel booking with ID: {}", currentUsername, bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        boolean isAdmin = authentication.getAuthorities().stream()
                             .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

        if (!booking.getUserId().equals(currentUsername) && !isAdmin) {
            logger.warn("BOOKING-SERVICE: User '{}' CANNOT cancel booking ID {}. Booking owned by '{}'.", currentUsername, bookingId, booking.getUserId());
            throw new AccessDeniedException("You are not authorized to cancel this booking.");
        }

        if (STATUS_CANCELLED.equalsIgnoreCase(booking.getBookingStatus())) {
            logger.warn("BOOKING-SERVICE: Booking ID {} is already cancelled.", bookingId);
            throw new BookingCancellationException("Booking is already cancelled.");
        }
        if (!CANCELLABLE_STATUSES.contains(booking.getBookingStatus().toUpperCase())) {
             String logMessage = String.format("Booking ID %d cannot be cancelled. Current status: '%s'. Allowed statuses for cancellation: %s",
                                     bookingId, booking.getBookingStatus(), CANCELLABLE_STATUSES);
             logger.warn("BOOKING-SERVICE: " + logMessage);
             throw new BookingCancellationException(logMessage);
        }
        booking.setBookingStatus(STATUS_CANCELLED);
        Booking cancelledBooking = bookingRepository.save(booking);
        logger.info("BOOKING-SERVICE: Booking ID {} successfully cancelled by user '{}'.", cancelledBooking.getId(), currentUsername);
        return mapToBookingResponseDto(cancelledBooking);
    }

    @Transactional(readOnly = true)
    public BookingResponseDto getBookingByIdForCurrentUser(Long bookingId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        logger.debug("BOOKING-SERVICE: User '{}' attempting to fetch booking with ID: {}", currentUsername, bookingId);

        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        boolean isAdmin = authentication.getAuthorities().stream()
                             .anyMatch(grantedAuthority -> ((GrantedAuthority) grantedAuthority).getAuthority().equals("ROLE_ADMIN"));

        if (!booking.getUserId().equals(currentUsername) && !isAdmin) {
            logger.warn("BOOKING-SERVICE: User '{}' access denied for booking ID {}. Booking owned by '{}'.", currentUsername, bookingId, booking.getUserId());
            throw new AccessDeniedException("You are not authorized to view this booking.");
        }
        logger.debug("BOOKING-SERVICE: Successfully fetched booking ID {} for user '{}'", bookingId, currentUsername);
        return mapToBookingResponseDto(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponseDto> getMyBookings() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String currentUserId = userDetails.getUsername();
        logger.info("BOOKING-SERVICE: Fetching all bookings for user '{}'", currentUserId);

        List<Booking> bookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(currentUserId);
        return bookings.stream()
                .map(this::mapToBookingResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookingResponseDto confirmBookingPayment(Long bookingId, String paymentReferenceId) {
        logger.info("BOOKING-SERVICE: Attempting to confirm payment for booking ID: {}, Payment Reference: {}", bookingId, paymentReferenceId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking for payment confirmation", "id", bookingId));

        if (!(STATUS_PENDING_PAYMENT.equalsIgnoreCase(booking.getBookingStatus()) ||
              STATUS_PAYMENT_INIT_FAILED.equalsIgnoreCase(booking.getBookingStatus()))) {
            logger.warn("BOOKING-SERVICE: Booking ID {} is not in a state to confirm payment. Current status: {}.",
                    bookingId, booking.getBookingStatus());
            return mapToBookingResponseDto(booking);
        }

        booking.setBookingStatus(STATUS_CONFIRMED);
        Booking updatedBooking = bookingRepository.save(booking);
        logger.info("BOOKING-SERVICE: Payment confirmed for Booking ID {}. Status updated to CONFIRMED.", updatedBooking.getId());
        return mapToBookingResponseDto(updatedBooking);
    }

    @Transactional
    public BookingResponseDto markBookingPaymentFailed(Long bookingId) {
        logger.info("BOOKING-SERVICE: Marking payment as FAILED for booking ID: {}", bookingId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking for payment failure update", "id", bookingId));

        if (STATUS_PENDING_PAYMENT.equalsIgnoreCase(booking.getBookingStatus()) ||
            STATUS_PAYMENT_INIT_FAILED.equalsIgnoreCase(booking.getBookingStatus())) {
            booking.setBookingStatus(STATUS_PAYMENT_FAILED);
            Booking updatedBooking = bookingRepository.save(booking);
            logger.info("BOOKING-SERVICE: Payment marked FAILED for Booking ID {}.", updatedBooking.getId());
            return mapToBookingResponseDto(updatedBooking);
        } else {
            logger.warn("BOOKING-SERVICE: Booking ID {} is not in a state to be marked as payment failed. Current status: {}.",
                    bookingId, booking.getBookingStatus());
            return mapToBookingResponseDto(booking);
        }
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