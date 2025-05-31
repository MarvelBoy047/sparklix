package com.sparklix.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentStatusDto {
    private Long internalBookingId;
    private String paymentId; // Razorpay payment ID or your internal payment ID
    private String status; // e.g., "CAPTURED", "FAILED", "AUTHORIZED", "SUCCESS"
    private String message;
}