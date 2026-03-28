package com.example.restaurant.repository.interfaces;

import com.example.restaurant.models.OrderItems;
import com.example.restaurant.models.Orders;
import com.example.restaurant.models.lookup.OrderItemsStatus;
import com.example.restaurant.models.lookup.OrderStatus;

import java.util.List;
import java.util.Optional;

public interface IOrderRepository {

    void assignWaiterToOrders(String reservationToken, String waiterToken);

    void isAbsent(String reservationToken);

    OrderStatus findStatusByToken(String token);

    void saveOrderWithItems(Orders order, List<OrderItems> items);

    Optional<Orders> findByReservationToken(String reservationToken);

    List<OrderItems> findItemsByOrderToken(String orderToken);

    void save(Orders order);

    void saveItem(OrderItems item);

    OrderItemsStatus findItemStatusByToken(String token);
}
