package com.example.restaurant.repository.interfaces.jpa;

import com.example.restaurant.models.Roles;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IJpaRoleRepository extends JpaRepository<Roles, UUID> {
    Optional<Roles> findByName(String name);
}
