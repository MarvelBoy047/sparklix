package com.sparklix.paymentservice.dto;

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
    private int amountInPaisa; // Amount in smallest currency unit (e.g. paisa)
    private String status; // e.g., "created"
    private String razorpayKeyId; // Your Razorpay Key ID
}