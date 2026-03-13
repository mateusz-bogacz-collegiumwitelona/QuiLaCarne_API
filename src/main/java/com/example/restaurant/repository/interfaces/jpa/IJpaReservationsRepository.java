package com.example.restaurant.repository.interfaces.jpa;

import com.example.restaurant.models.Reservations;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IJpaReservationsRepository extends JpaRepository<Reservations, UUID> {
    Optional<Reservations> findByToken(String token);

    List<Reservations> findAllByUser_Token(String userToken);
}
