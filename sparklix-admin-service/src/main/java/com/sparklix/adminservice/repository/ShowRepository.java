package com.sparklix.adminservice.repository;

import com.sparklix.adminservice.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {
    boolean existsByTitle(String title);
}