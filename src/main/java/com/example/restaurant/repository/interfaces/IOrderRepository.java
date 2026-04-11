package com.example.restaurant.repository.interfaces;

import com.example.restaurant.models.OrderItems;
import com.example.restaurant.models.Orders;
import com.example.restaurant.models.lookup.OrderItemsStatus;
import com.example.restaurant.models.lookup.OrderStatus;

import java.util.List;
import java.util.Optional;

public interface IOrderRepository {

    OrderStatus findStatusByToken(String token);

    void saveOrderWithItems(Orders order, List<OrderItems> items);

    Optional<Orders> findByReservationToken(String reservationToken);

    List<OrderItems> findItemsByOrderToken(String orderToken);

    void save(Orders order);

    void saveItem(OrderItems item);

    OrderItemsStatus findItemStatusByToken(String token);

    void saveAllItems(List<OrderItems> items);

    List<OrderStatus> findAllStatuses();

    List<OrderItemsStatus> findAllItemStatuses();

    boolean isStatusNameTaken(String pl, String en);

    void saveStatus(OrderStatus status);

    boolean isItemStatusNameTaken(String pl, String en);

    void saveItemStatus(OrderItemsStatus status);

    List<Orders> findOrdersByStatus(OrderStatus status);

    List<OrderItems> findOrderItemsByStatus(OrderItemsStatus status);

    long countOrderItemsStatuses();

    long countStatuses();

    long countItems();

    long count();
}
