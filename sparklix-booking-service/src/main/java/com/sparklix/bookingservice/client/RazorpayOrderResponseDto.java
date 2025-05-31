package com.sparklix.bookingservice.client; // Ensure package is correct

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayOrderResponseDto {
    private String razorpayOrderId;
    private Long internalBookingId;
    private String currency;
    private int amountInPaisa;
    private String status; // e.g., "created" from Razorpay
    private String razorpayKeyId;
}