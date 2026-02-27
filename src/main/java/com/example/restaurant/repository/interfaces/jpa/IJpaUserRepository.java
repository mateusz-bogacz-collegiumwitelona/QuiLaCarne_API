package com.example.restaurant.repository.interfaces.jpa;

import com.example.restaurant.models.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IJpaUserRepository extends JpaRepository<Users, UUID> {
    Optional<Users> findByUsername(String username);
    Optional<Users> findByToken(String token);
}
