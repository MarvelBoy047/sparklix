package com.sparklix.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayOrderRequestDto {
    private Long bookingId;
    private BigDecimal amount; // This DTO expects actual amount, service will convert to paisa
    private String currency; // e.g., "INR"
    private String receipt;
    private String userId;
}