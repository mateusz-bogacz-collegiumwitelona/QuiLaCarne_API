package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.domain.ReservationDomain;
import com.example.restaurant.dto.request.ReservationDishRequest;

import java.util.List;

public interface IOrderServices {
    ReservationDomain createOrderForReservation(
            String reservationToken,
            String tableToken,
            List<ReservationDishRequest> dishesRequest
    );
}
