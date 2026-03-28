package com.example.restaurant.repository;

import com.example.restaurant.models.OrderItems;
import com.example.restaurant.models.Orders;
import com.example.restaurant.models.lookup.OrderItemsStatus;
import com.example.restaurant.models.lookup.OrderStatus;
import com.example.restaurant.repository.interfaces.IOrderRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaOrderItemStatusRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaOrderItemsRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaOrderRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaOrederStatusRepositry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderRepository implements IOrderRepository {
    private final IJpaOrderRepository _jpaOrderRepo;
    private final IJpaOrderItemsRepository _jpaOrderItemRepo;
    private final IJpaOrederStatusRepositry _jpaOrderStatusRepo;
    private final IJpaOrderItemStatusRepository _jpaOrderItemStatusRepo;
    
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

    @Override
    public void saveAllItems(List<OrderItems> items) {
        _jpaOrderItemRepo.saveAll(items);
    }
}
