package com.sparklix.bookingservice.controller;

import com.sparklix.bookingservice.dto.BookingRequestDto;
import com.sparklix.bookingservice.dto.BookingResponseDto;
import com.sparklix.bookingservice.service.BookingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'VENDOR', 'ADMIN')")
    public ResponseEntity<BookingResponseDto> createBooking(@Valid @RequestBody BookingRequestDto bookingRequestDto) {
        logger.info("BOOKING-CONTROLLER: Received request to create booking: {}", bookingRequestDto);
        BookingResponseDto createdBooking = bookingService.createBooking(bookingRequestDto);
        logger.info("BOOKING-CONTROLLER: Booking created successfully with ID: {}, initial status: {}",
                createdBooking.getBookingId(), createdBooking.getBookingStatus());
        return new ResponseEntity<>(createdBooking, HttpStatus.CREATED);
    }

    @GetMapping("/my-bookings")
    @PreAuthorize("hasAnyRole('USER', 'VENDOR')")
    public ResponseEntity<List<BookingResponseDto>> getMyBookings() {
        logger.info("BOOKING-CONTROLLER: Received request to get my bookings.");
        List<BookingResponseDto> bookings = bookingService.getMyBookings();
        logger.info("BOOKING-CONTROLLER: Returning {} bookings.", bookings.size());
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/{bookingId}")
    @PreAuthorize("hasAnyRole('USER', 'VENDOR', 'ADMIN')")
    public ResponseEntity<BookingResponseDto> getBookingById(@PathVariable("bookingId") Long bookingId) {
        logger.info("BOOKING-CONTROLLER: Received request to get booking by ID: {}", bookingId);
        BookingResponseDto booking = bookingService.getBookingByIdForCurrentUser(bookingId);
        logger.info("BOOKING-CONTROLLER: Returning booking: {}", booking.getBookingId());
        return ResponseEntity.ok(booking);
    }

    @PutMapping("/{bookingId}/cancel")
    @PreAuthorize("hasAnyRole('USER', 'VENDOR', 'ADMIN')")
    public ResponseEntity<BookingResponseDto> cancelBooking(@PathVariable("bookingId") Long bookingId) {
        logger.info("BOOKING-CONTROLLER: Received request to cancel booking by ID: {}", bookingId);
        BookingResponseDto cancelledBooking = bookingService.cancelBooking(bookingId);
        logger.info("BOOKING-CONTROLLER: Booking {} status after cancellation attempt: {}",
                cancelledBooking.getBookingId(), cancelledBooking.getBookingStatus());
        return ResponseEntity.ok(cancelledBooking);
    }

    // --- NEW Endpoints for Payment Service Callbacks ---
    // These should be secured for service-to-service communication
    // For now, using ROLE_ADMIN as a placeholder for a service role.
    // In a real scenario, you might use client credentials or a dedicated service role.

    @PutMapping("/{bookingId}/internal/confirm-payment")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_SERVICE_ACCOUNT')") // TODO: Refine security for S2S
    public ResponseEntity<BookingResponseDto> confirmPaymentForBooking(
            @PathVariable("bookingId") Long bookingId,
            @RequestParam("paymentReferenceId") String paymentReferenceId) {
        logger.info("BOOKING-CONTROLLER: Received internal callback to confirm payment for bookingId: {} with paymentRef: {}",
                bookingId, paymentReferenceId);
        BookingResponseDto booking = bookingService.confirmBookingPayment(bookingId, paymentReferenceId);
        return ResponseEntity.ok(booking);
    }

    @PutMapping("/{bookingId}/internal/payment-failed")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_SERVICE_ACCOUNT')") // TODO: Refine security for S2S
    public ResponseEntity<BookingResponseDto> paymentFailedForBooking(
            @PathVariable("bookingId") Long bookingId) {
        logger.info("BOOKING-CONTROLLER: Received internal callback for failed payment for bookingId: {}", bookingId);
        BookingResponseDto booking = bookingService.markBookingPaymentFailed(bookingId);
        return ResponseEntity.ok(booking);
    }
}