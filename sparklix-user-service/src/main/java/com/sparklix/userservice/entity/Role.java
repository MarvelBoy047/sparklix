package com.sparklix.userservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name; // e.g., ROLE_USER, ROLE_ADMIN, ROLE_VENDOR

    // If you want a bidirectional relationship (optional for now, but useful later)
    // This side is often the "inverse" side and not strictly needed for User->Role mapping
    // @ManyToMany(mappedBy = "roles")
    // private Set<User> users;

    // Constructor without id for creating new roles before saving
    public Role(String name) {
        this.name = name;
    }
}