package com.example.restaurant.repository.interfaces.jpa;

import com.example.restaurant.models.Reservations;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IJpaReservationsRepository
    extends JpaRepository<Reservations, UUID>, JpaSpecificationExecutor<Reservations> {
  Optional<Reservations> findByToken(String token);

  Optional<Reservations> findByTokenAndUser_Token(String token, String userToken);

  @Query(
      "SELECT r FROM Reservations r JOIN r.reservationStatus s WHERE s.token = 'ACTIVE' AND r.startTime < :deadline")
  List<Reservations> findExpiredActiveReservations(@Param("deadline") OffsetDateTime deadline);
}
