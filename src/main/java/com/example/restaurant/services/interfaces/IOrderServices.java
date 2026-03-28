package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.domain.OrderSummaryDomain;
import com.example.restaurant.dto.domain.ReservationDomain;
import com.example.restaurant.dto.domain.TodayOrderSummaryDomain;
import com.example.restaurant.dto.request.ReservationDishRequest;

import java.util.List;

public interface IOrderServices {
    ReservationDomain createOrderForReservation(
            String reservationToken,
            String tableToken,
            List<ReservationDishRequest> dishesRequest
    );

    OrderSummaryDomain getOrderSummaryForReservation(String reservationToken);

    TodayOrderSummaryDomain todayOrderDetails(String reservationToken, String lang);

    void removeItemFromReservation(String waiterToken, String reservationToken, ReservationDishRequest request);

    void addItemFromReservation(String waiterToken, String reservationToken, List<ReservationDishRequest> request);
}
