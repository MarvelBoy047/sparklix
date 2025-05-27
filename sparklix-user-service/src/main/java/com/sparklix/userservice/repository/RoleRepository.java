package com.sparklix.userservice.repository;

import com.sparklix.userservice.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    // Spring Data JPA will automatically implement a method based on its name
    // This method will find a Role by its name
    Optional<Role> findByName(String name);

    // You already get these methods from JpaRepository:
    // - save(Role role)
    // - findById(Long id)
    // - findAll()
    // - deleteById(Long id)
    // - count()
    // - existsById(Long id)
    // ... and many more
}