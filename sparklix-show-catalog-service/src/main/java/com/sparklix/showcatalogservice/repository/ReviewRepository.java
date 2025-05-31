package com.sparklix.showcatalogservice.repository;

import com.sparklix.showcatalogservice.entity.Review;
// No need to import Show here if we use showId
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository("catalogReviewRepository")
public interface ReviewRepository extends JpaRepository<Review, Long> {
    // Find by local catalog Show ID (this 'showId' refers to Review.show.id)
    List<Review> findByShow_Id(Long catalogShowId); 

    // Find a review by local catalog Show ID and userId
    Optional<Review> findByShow_IdAndUserId(Long catalogShowId, String userId);

    // Check if a review exists by local catalog Show ID and userId
    boolean existsByShow_IdAndUserId(Long catalogShowId, String userId); // <-- MODIFIED: Takes Long showId
}