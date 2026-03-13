package com.example.restaurant.repository;

import com.example.restaurant.dto.request.ClientReservationRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.request.ReservationRequest;
import com.example.restaurant.dto.response.ClientReservationResponse;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.mappers.ReservationMapper;
import com.example.restaurant.models.Reservations;
import com.example.restaurant.models.RestaurantTables;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.lookup.ReservationStatus;
import com.example.restaurant.repository.interfaces.IReservationRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaReservationStatusRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaReservationsRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaTableRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaUserRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class ReservationRepository implements IReservationRepository {
    private final IJpaUserRepository _jpaUserRepo;
    private final IJpaTableRepository _jpaTableRepo;
    private final IJpaReservationStatusRepository _jpaReservationStatusRepo;
    private final IJpaReservationsRepository _jpaReservationsRepo;
    private final ReservationMapper _reservationMapper;

    @Override
    @Transactional
    public String createReservation(ReservationRequest request, String userToken) {
        Users user = _jpaUserRepo.findByToken(userToken)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RestaurantTables table = _jpaTableRepo.findByToken(request.getTableToken());
        if (table == null) throw new RuntimeException("Table not found");

        ReservationStatus activeStatus = _jpaReservationStatusRepo.findByToken("ACTIVE")
                .orElseThrow(() -> new RuntimeException("ReservationStatus not found"));

        Reservations reservation = new Reservations();
        reservation.setUser(user);
        reservation.setTableId(table);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setReservationStatus(Set.of(activeStatus));

        _jpaReservationsRepo.saveAndFlush(reservation);

        return reservation.getToken();
    }

    @Override
    public PagedResult<ClientReservationResponse> history(String userToken, String lang, ClientReservationRequest filter, PaggedRequest pagged) {
        Pageable pageable = PageRequest.of(pagged.getPage() - 1, pagged.getSize(), Sort.by(Sort.Direction.ASC, "startTime"));

        Specification<Reservations> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("user").get("token"), userToken));

            if (filter.getFromDate() != null)
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("startTime"), filter.getFromDate()));

            if (filter.getToDate() != null)
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("startTime"), filter.getToDate()));

            if (filter.getStatusToken() != null && !filter.getStatusToken().isEmpty()) {
                Join<Reservations, ReservationStatus> statusJoin = root.join("reservationStatus");
                predicates.add(criteriaBuilder.equal(statusJoin.get("token"), filter.getStatusToken()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<Reservations> reservationPage = _jpaReservationsRepo.findAll(spec, pageable);

        Page<ClientReservationResponse> dtoPage = reservationPage
                .map(res -> _reservationMapper.toClientReservationResponse(res, lang));

        return new PagedResult<>(dtoPage);
    }
}
