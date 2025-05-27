package com.sparklix.adminservice.repository;

import com.sparklix.adminservice.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VenueRepository extends JpaRepository<Venue, Long> {
    boolean existsByNameAndCity(String name, String city);
}