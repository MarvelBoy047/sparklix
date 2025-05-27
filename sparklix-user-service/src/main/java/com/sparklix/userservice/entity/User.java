package com.sparklix.userservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;     // <--- IMPORTANT
import lombok.NoArgsConstructor;
import lombok.Setter;     // <--- IMPORTANT (or use @Data)
// Alternatively, you can replace @Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor with just @Data
// import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter             // Ensure this is present
@Setter             // Ensure this is present
@NoArgsConstructor
@AllArgsConstructor
// @Data // If you use @Data, you can remove @Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor if not needed separately
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String username; // Needs getUsername()

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 128)
    private String password; // Needs getPassword()

    private boolean enabled = true; // Needs isEnabled()

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id")
    )
    private Set<Role> roles = new HashSet<>(); // Needs getRoles()
}