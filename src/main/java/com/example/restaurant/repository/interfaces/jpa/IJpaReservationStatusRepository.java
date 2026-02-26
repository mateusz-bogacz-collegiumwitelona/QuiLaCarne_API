package com.example.restaurant.repository.interfaces.jpa;

import com.example.restaurant.models.named.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IJpaReservationStatusRepository extends JpaRepository<ReservationStatus, UUID> {
    Optional<ReservationStatus> findByName(String name);
}
