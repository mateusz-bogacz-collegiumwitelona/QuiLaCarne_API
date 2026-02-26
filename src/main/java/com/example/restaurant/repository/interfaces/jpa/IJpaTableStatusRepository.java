package com.example.restaurant.repository.interfaces.jpa;

import com.example.restaurant.models.TableStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IJpaTableStatusRepository extends JpaRepository<TableStatus, UUID> {
    Optional<TableStatus> findByName(String name);
}
