package com.sparklix.paymentservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentVerificationRequestDto {
    private String razorpay_order_id;
    private String razorpay_payment_id;
    private String razorpay_signature;
    private Long internalBookingId;
}