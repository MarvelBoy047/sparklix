package com.sparklix.userservice.repository;

import com.sparklix.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find a user by their username
    // Spring Data JPA generates the query: SELECT u FROM User u WHERE u.username = ?1
    Optional<User> findByUsername(String username);

    // Find a user by their email
    // Spring Data JPA generates the query: SELECT u FROM User u WHERE u.email = ?1
    Optional<User> findByEmail(String email);

    // Check if a user exists by username
    // Spring Data JPA generates the query: SELECT CASE WHEN COUNT(u) > 0 THEN TRUE ELSE FALSE END FROM User u WHERE u.username = ?1
    Boolean existsByUsername(String username);

    // Check if a user exists by email
    // Spring Data JPA generates the query: SELECT CASE WHEN COUNT(u) > 0 THEN TRUE ELSE FALSE END FROM User u WHERE u.email = ?1
    Boolean existsByEmail(String email);

    // JpaRepository already provides methods like:
    // - save(User user)
    // - findById(Long id)
    // - findAll()
    // - delete(User user)
    // - etc.
}