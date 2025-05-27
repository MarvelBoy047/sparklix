package com.sparklix.showcatalogservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponseDto {
    private Long id;
    private Long showId;      // Local catalog_show_id
    private String showTitle; // Denormalized for convenience
    private String userId;
    private int rating;
    private String comment;
    private LocalDateTime reviewDate;
}