package com.sparklix.showcatalogservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // ADDED
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "venues_catalog")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // ADDED
public class Venue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true) // originalVenueId should be unique in this catalog table
    private Long originalVenueId;

    private String name;
    private String address;
    private String city;
    private int capacity;
}