package com.sparklix.bookingservice.client; // Ensure package is correct

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayOrderRequestDto {
    private Long bookingId;
    private BigDecimal amount; // Actual amount, payment service will convert to paisa
    private String currency;
    private String receipt;
    private String userId;
}