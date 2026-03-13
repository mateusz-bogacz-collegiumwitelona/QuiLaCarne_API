package com.example.restaurant.repository.interfaces;

import com.example.restaurant.dto.domain.OrderSummaryDomain;
import com.example.restaurant.dto.domain.ReservationDomain;
import com.example.restaurant.dto.request.ReservationDishRequest;

import java.util.List;

public interface IOrderRepository {
    ReservationDomain createOrderForReservation(String reservationToken, String tableToken, List<ReservationDishRequest> dishesRequest);

    OrderSummaryDomain getOrderSummaryForReservation(String reservationToken);
}
