package com.example.restaurant.repository.interfaces.jpa;

import com.example.restaurant.models.Reservations;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IJpaReservationsRepository
    extends JpaRepository<Reservations, UUID>, JpaSpecificationExecutor<Reservations> {
  Optional<Reservations> findByToken(String token);

  Optional<Reservations> findByTokenAndUser_Token(String token, String userToken);
}
