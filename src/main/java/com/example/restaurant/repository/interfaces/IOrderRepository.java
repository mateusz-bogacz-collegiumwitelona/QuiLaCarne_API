package com.example.restaurant.repository.interfaces;

import com.example.restaurant.dto.domain.OrderSummaryDomain;
import com.example.restaurant.dto.domain.TodayOrderSummaryDomain;
import com.example.restaurant.dto.request.ReservationDishRequest;
import com.example.restaurant.models.OrderItems;
import com.example.restaurant.models.Orders;
import com.example.restaurant.models.lookup.OrderStatus;

import java.util.List;

public interface IOrderRepository {
    OrderSummaryDomain getOrderSummaryForReservation(String reservationToken);

    TodayOrderSummaryDomain todayOrderDetails(String reservationToken, String lang);

    void removeItemFromReservation(String waiterToken, String reservationToken, ReservationDishRequest request);

    void addItemFromReservation(String waiterToken, String reservationToken, List<ReservationDishRequest> request);

    void assignWaiterToOrders(String reservationToken, String waiterToken);

    void isAbsent(String reservationToken);

    OrderStatus findStatusByToken(String token);

    void saveOrderWithItems(Orders order, List<OrderItems> items);
}
