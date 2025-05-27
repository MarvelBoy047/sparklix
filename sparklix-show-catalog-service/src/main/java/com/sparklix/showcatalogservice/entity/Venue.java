package com.sparklix.showcatalogservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity @Table(name = "venues_catalog") @Data @NoArgsConstructor @AllArgsConstructor
public class Venue {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private Long originalVenueId; // Store the ID from admin-service
    private String name;
    private String address;
    private String city;
    private int capacity;
}