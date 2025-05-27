package com.sparklix.adminservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ShowRequestDto {
    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    private String description;

    @NotBlank(message = "Genre is required")
    @Size(min = 2, max = 50, message = "Genre must be between 2 and 50 characters")
    private String genre;

    @NotBlank(message = "Language is required")
    @Size(min = 2, max = 50, message = "Language must be between 2 and 50 characters")
    private String language;

    @Min(value = 1, message = "Duration must be at least 1 minute")
    private int durationMinutes;

    @FutureOrPresent(message = "Release date must be in the present or future")
    private LocalDate releaseDate;

    @Pattern(regexp = "^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w .-]*)*/?$", message = "Invalid poster URL format")
    private String posterUrl;
}