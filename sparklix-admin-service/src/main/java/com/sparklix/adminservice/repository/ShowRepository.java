package com.sparklix.adminservice.repository;

import com.sparklix.adminservice.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShowRepository extends JpaRepository<Show, Long> {
    boolean existsByTitle(String title);
    // Add this new method:
    boolean existsByTitleAndIdNot(String title, Long id);
}