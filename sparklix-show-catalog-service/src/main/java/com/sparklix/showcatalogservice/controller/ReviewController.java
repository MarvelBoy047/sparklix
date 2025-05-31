package com.sparklix.showcatalogservice.controller;

import com.sparklix.showcatalogservice.dto.ReviewRequestDto;
import com.sparklix.showcatalogservice.dto.ReviewResponseDto;
import com.sparklix.showcatalogservice.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shows/{showId}/reviews") // This maps all methods in this controller to this base path
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping // Maps to POST /api/shows/{showId}/reviews
    @PreAuthorize("hasAnyRole('USER', 'VENDOR')") 
    public ResponseEntity<ReviewResponseDto> addReview(@PathVariable Long showId,
                                                       @Valid @RequestBody ReviewRequestDto reviewRequestDto) {
        ReviewResponseDto createdReview = reviewService.addReview(showId, reviewRequestDto);
        return new ResponseEntity<>(createdReview, HttpStatus.CREATED);
    }

    @GetMapping // Maps to GET /api/shows/{showId}/reviews
    public ResponseEntity<List<ReviewResponseDto>> getReviewsForShow(@PathVariable Long showId) {
        List<ReviewResponseDto> reviews = reviewService.getReviewsForShow(showId);
        return ResponseEntity.ok(reviews);
    }
}