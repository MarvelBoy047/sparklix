package com.sparklix.showcatalogservice.service;

import com.sparklix.showcatalogservice.dto.ReviewRequestDto;
import com.sparklix.showcatalogservice.dto.ReviewResponseDto;
import com.sparklix.showcatalogservice.entity.Review;
import com.sparklix.showcatalogservice.entity.Show;
import com.sparklix.showcatalogservice.exception.ResourceConflictException;
import com.sparklix.showcatalogservice.exception.ResourceNotFoundException;
import com.sparklix.showcatalogservice.repository.ReviewRepository;
import com.sparklix.showcatalogservice.repository.ShowRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ShowRepository showRepository;

    public ReviewService(ReviewRepository reviewRepository, ShowRepository showRepository) {
        this.reviewRepository = reviewRepository;
        this.showRepository = showRepository;
    }

    @Transactional
    public ReviewResponseDto addReview(Long localShowId, ReviewRequestDto reviewRequestDto) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userId = userDetails.getUsername();

        Show show = showRepository.findById(localShowId)
                .orElseThrow(() -> new ResourceNotFoundException("Show", "id", localShowId));

        // CORRECTED: Call the method with the underscore
        if (reviewRepository.existsByShow_IdAndUserId(localShowId, userId)) {
            throw new ResourceConflictException("You have already reviewed this show.");
        }

        Review review = new Review();
        review.setShow(show);
        review.setUserId(userId);
        review.setRating(reviewRequestDto.getRating());
        review.setComment(reviewRequestDto.getComment());
        // reviewDate is set by @PrePersist in Review entity

        Review savedReview = reviewRepository.save(review);
        return mapToDto(savedReview);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getReviewsForShow(Long localShowId) {
        if (!showRepository.existsById(localShowId)) {
             throw new ResourceNotFoundException("Show", "id", localShowId);
        }
        // CORRECTED: Call the method with the underscore
        return reviewRepository.findByShow_Id(localShowId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private ReviewResponseDto mapToDto(Review review) {
        ReviewResponseDto dto = new ReviewResponseDto();
        dto.setId(review.getId());
        if (review.getShow() != null) {
            dto.setShowId(review.getShow().getId());
            dto.setShowTitle(review.getShow().getTitle());
        }
        dto.setUserId(review.getUserId());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setReviewDate(review.getReviewDate());
        return dto;
    }
}