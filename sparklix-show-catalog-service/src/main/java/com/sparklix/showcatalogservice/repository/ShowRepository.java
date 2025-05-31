package com.sparklix.showcatalogservice.repository;

import com.sparklix.showcatalogservice.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository("catalogShowRepository")
public interface ShowRepository extends JpaRepository<Show, Long> {
    
    List<Show> findByGenre(String genre);
    List<Show> findByTitleContainingIgnoreCase(String titleKeyword);
    Optional<Show> findByOriginalShowId(Long originalShowId);
}