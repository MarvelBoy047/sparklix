package com.sparklix.adminservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VenueRequestDto {
    @NotBlank(message = "Venue name is required")
    @Size(min = 2, max = 100, message = "Venue name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Address is required")
    @Size(min = 5, max = 255, message = "Address must be between 5 and 255 characters")
    private String address;

    @NotBlank(message = "City is required")
    @Size(min = 2, max = 50, message = "City must be between 2 and 50 characters")
    private String city;

    @Min(value = 1, message = "Capacity must be at least 1")
    private int capacity;
}