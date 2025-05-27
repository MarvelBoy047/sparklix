// In com.sparklix.showcatalogservice.dto.ShowtimeDetailsDto.java
package com.sparklix.showcatalogservice.dto;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShowtimeDetailsDto {
    private Long originalShowtimeId;
    private String showTitle;
    private String venueName;
    private LocalDateTime showDateTime;
    private BigDecimal pricePerSeat;
    private int availableSeats; // This field name is what the constructor will expect
}