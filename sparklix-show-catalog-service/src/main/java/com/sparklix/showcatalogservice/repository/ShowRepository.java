package com.sparklix.showcatalogservice.repository;

import com.sparklix.showcatalogservice.entity.Show; // Uses local Show entity
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional; // Add this import

@Repository("catalogShowRepository") // Optional: give it a distinct name
public interface ShowRepository extends JpaRepository<Show, Long> {
    // Existing methods from your previous setup
    List<Show> findByGenre(String genre);
    List<Show> findByTitleContainingIgnoreCase(String titleKeyword);

    // Add this if you want to find a local show by the original ID from the admin service
    Optional<Show> findByOriginalShowId(Long originalShowId);
}