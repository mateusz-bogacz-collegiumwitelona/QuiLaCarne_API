package com.example.restaurant.repository;

import com.example.restaurant.dto.request.ReservationRequest;
import com.example.restaurant.dto.response.ClientReservationResponse;
import com.example.restaurant.models.Reservations;
import com.example.restaurant.models.RestaurantTables;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.lookup.ReservationStatus;
import com.example.restaurant.repository.interfaces.IReservationRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaReservationStatusRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaReservationsRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaTableRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaUserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class ReservationRepository implements IReservationRepository {
    private final IJpaUserRepository _jpaUserRepo;
    private final IJpaTableRepository _jpaTableRepo;
    private final IJpaReservationStatusRepository _jpaReservationStatusRepo;
    private final IJpaReservationsRepository _jpaReservationsRepo;

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
    public List<ClientReservationResponse> history(String userToken, String lang) {
        var reservations = _jpaReservationsRepo.findAllByUser_Token(userToken);

        return reservations.stream()
                .map(res -> {
                            String statusName = "UNKNOWN";

                            if (res.getReservationStatus() != null &&
                                    !res.getReservationStatus().isEmpty()) {
                                var status = res.getReservationStatus().iterator().next();
                                statusName = status.translate(lang);
                            }

                            return ClientReservationResponse.builder()
                                    .token(res.getToken())
                                    .startTime(res.getStartTime())
                                    .endTime(res.getEndTime())
                                    .status(statusName)
                                    .build();
                        }
                ).toList();
    }
}
