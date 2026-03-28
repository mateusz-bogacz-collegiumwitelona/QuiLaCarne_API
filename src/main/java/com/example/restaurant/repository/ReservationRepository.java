package com.example.restaurant.repository;

import com.example.restaurant.exceptions.ReservationStatusNotFoundException;
import com.example.restaurant.models.Reservations;
import com.example.restaurant.models.lookup.ReservationStatus;
import com.example.restaurant.repository.interfaces.IReservationRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaReservationStatusRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaReservationsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReservationRepository implements IReservationRepository {
    private final IJpaReservationStatusRepository _jpaReservationStatusRepo;
    private final IJpaReservationsRepository _jpaReservationsRepo;

    @Override
    public ReservationStatus findStatusByToken(String token) {
        return _jpaReservationStatusRepo.findByToken(token)
                .orElseThrow(() -> new ReservationStatusNotFoundException("Reservation not found"));
    }

    @Override
    public void save(Reservations reservation) {
        _jpaReservationsRepo.saveAndFlush(reservation);
    }

    @Override
    public Page<Reservations> findAll(Specification<Reservations> spec, Pageable pageable) {
        return _jpaReservationsRepo.findAll(spec, pageable);
    }

    @Override
    public Optional<Reservations> findByToken(String token) {
        return _jpaReservationsRepo.findByToken(token);
    }

    @Override
    public Optional<Reservations> findByTokenAndUserToken(String resToken, String userToken) {
        return _jpaReservationsRepo.findByTokenAndUser_Token(resToken, userToken);
    }
}
