package com.example.restaurant.repository.interfaces;

import com.example.restaurant.models.Reservations;
import com.example.restaurant.models.lookup.ReservationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface IReservationRepository {

  ReservationStatus findStatusByToken(String token);

  void save(Reservations reservation);

  Page<Reservations> findAll(Specification<Reservations> spec, Pageable pageable);

  Optional<Reservations> findByToken(String token);

  Optional<Reservations> findByTokenAndUserToken(String resToken, String userToken);

  List<ReservationStatus> findAllStatuses();

  long countStatuses();

  long count();
}
