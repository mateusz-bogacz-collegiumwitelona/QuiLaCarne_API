package com.example.restaurant.repository.interfaces;

import com.example.restaurant.dto.request.ReservationRequest;

public interface IReservationRepository {
    String createReservation(ReservationRequest request, String userToken);
}
