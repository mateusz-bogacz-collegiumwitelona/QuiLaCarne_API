package com.example.restaurant.repository;

import com.example.restaurant.exceptions.ReservationNotFoundException;
import com.example.restaurant.exceptions.UserNotFoundException;
import com.example.restaurant.models.OrderItems;
import com.example.restaurant.models.Orders;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.lookup.OrderItemsStatus;
import com.example.restaurant.models.lookup.OrderStatus;
import com.example.restaurant.repository.interfaces.IOrderRepository;
import com.example.restaurant.repository.interfaces.jpa.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class OrderRepository implements IOrderRepository {
    private final IJpaOrderRepository _jpaOrderRepo;
    private final IJpaOrderItemsRepository _jpaOrderItemRepo;
    private final IJpaOrederStatusRepositry _jpaOrderStatusRepo;
    private final IJpaOrderItemStatusRepository _jpaOrderItemStatusRepo;
    private final IJpaUserRepository _jpaUserRepo;


    @Transactional
    @Override
    public void assignWaiterToOrders(String reservationToken, String waiterToken) {
        Orders order = _jpaOrderRepo.findByReservation_Token(reservationToken)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        Users waiter = _jpaUserRepo.findByToken(waiterToken).orElseThrow(() -> new UserNotFoundException("Waiter not found"));

        OrderStatus orderStatus = _jpaOrderStatusRepo.findByToken("IN_PROGRESS").orElseThrow(
                () -> new RuntimeException("Order status not found"));

        OrderItemsStatus orderItemsStatus = _jpaOrderItemStatusRepo.findByToken("IN_PROGRESS").orElseThrow(
                () -> new RuntimeException("Order item status not found"));

        order.setStatuses(new HashSet<>(Set.of(orderStatus)));
        order.setWaiter(waiter);

        List<OrderItems> items = _jpaOrderItemRepo.findAllByOrder_Token(order.getToken());

        for (OrderItems item : items) {
            boolean isPending = item.getStatuses()
                    .stream()
                    .anyMatch(s -> s.getToken().equals("PENDING"));

            if (isPending || item.getStatuses().isEmpty()) item.setStatuses(new HashSet<>(Set.of(orderItemsStatus)));
        }

        _jpaOrderItemRepo.saveAll(items);
        _jpaOrderRepo.saveAndFlush(order);
    }

    @Override
    public void isAbsent(String reservationToken) {
        _jpaOrderRepo.findByReservation_Token(reservationToken).ifPresent(order -> {
            OrderStatus orderStatus = _jpaOrderStatusRepo.findByToken("CANCELLED")
                    .orElseThrow(() -> new RuntimeException("Order status not found"));

            OrderItemsStatus orderItemsStatus = _jpaOrderItemStatusRepo.findByToken("CANCELLED")
                    .orElseThrow(() -> new RuntimeException("Order item status not found"));

            order.setStatuses(new HashSet<>(Set.of(orderStatus)));

            List<OrderItems> items = _jpaOrderItemRepo.findAllByOrder_Token(order.getToken());
            for (OrderItems item : items) {
                item.setStatuses(new HashSet<>(Set.of(orderItemsStatus)));
            }

            _jpaOrderItemRepo.saveAll(items);
            _jpaOrderRepo.saveAndFlush(order);
        });
    }

    @Override
    public OrderStatus findStatusByToken(String token) {
        return _jpaOrderStatusRepo.findByToken(token).orElseThrow(
                () -> new RuntimeException("Order status not found")
        );
    }

    @Override
    public void saveOrderWithItems(Orders order, List<OrderItems> items) {
        _jpaOrderRepo.saveAndFlush(order);
        _jpaOrderItemRepo.saveAllAndFlush(items);
    }

    @Override
    public Optional<Orders> findByReservationToken(String reservationToken) {
        return _jpaOrderRepo.findByReservation_Token(reservationToken);
    }

    @Override
    public List<OrderItems> findItemsByOrderToken(String orderToken) {
        return _jpaOrderItemRepo.findAllByOrder_Token(orderToken);
    }

    @Override
    public void save(Orders order) {
        _jpaOrderRepo.saveAndFlush(order);
    }

    @Override
    public void saveItem(OrderItems item) {
        _jpaOrderItemRepo.save(item);
    }

    @Override
    public OrderItemsStatus findItemStatusByToken(String token) {
        return _jpaOrderItemStatusRepo.findByToken(token).orElseThrow(
                () -> new RuntimeException("Order status not found")
        );
    }
}
