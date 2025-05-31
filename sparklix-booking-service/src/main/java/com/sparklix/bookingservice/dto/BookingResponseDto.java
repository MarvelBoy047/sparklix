package com.sparklix.bookingservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponseDto {
    private Long bookingId;
    private String userId;
    private Long originalShowtimeId;
    private String showTitle;
    private String venueName;
    private LocalDateTime showDateTime;
    private int numberOfTickets;
    private BigDecimal totalPrice;
    private String bookingStatus;
    private LocalDateTime createdAt;
}