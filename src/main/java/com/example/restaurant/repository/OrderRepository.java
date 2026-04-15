package com.example.restaurant.repository;

import com.example.restaurant.exceptions.EntityNotFoundException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@SuppressWarnings({"PMD.TooManyMethods", "PMD.CouplingBetweenObjects", "PMD.GodClass"})
public class OrderRepository implements IOrderRepository {
    private final IJpaOrderRepository _jpaOrderRepo;
    private final IJpaOrderItemsRepository _jpaOrderItemRepo;
    private final IJpaOrederStatusRepositry _jpaOrderStatusRepo;
    private final IJpaOrderItemStatusRepository _jpaOrderItemStatusRepo;

    @Override
    public Page<Orders> findAll(Pageable pageable) {
        return _jpaOrderRepo.findAll(pageable);
    }

    @Override
    public OrderStatus findStatusByToken(String token) {
        return _jpaOrderStatusRepo.findByToken(token).orElseThrow(
                () -> new EntityNotFoundException("Order status not found")
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
                () -> new EntityNotFoundException("Order status not found")
        );
    }

    @Override
    public void saveAllItems(List<OrderItems> items) {
        _jpaOrderItemRepo.saveAll(items);
    }

    @Override
    public List<OrderStatus> findAllStatuses() {
        return _jpaOrderStatusRepo.findAll();
    }

    @Override
    public List<OrderItemsStatus> findAllItemStatuses() {
        return _jpaOrderItemStatusRepo.findAll();
    }

    @Override
    public boolean isStatusNameTaken(String pl, String en) {
        return _jpaOrderStatusRepo.findByNamePl(pl).isPresent() ||
                _jpaOrderStatusRepo.findByNameEn(en).isPresent();
    }

    @Override
    public void saveStatus(OrderStatus status) {
        _jpaOrderStatusRepo.save(status);
    }

    @Override
    public boolean isItemStatusNameTaken(String pl, String en) {
        return _jpaOrderItemStatusRepo.findByNamePl(pl).isPresent() ||
                _jpaOrderItemStatusRepo.findByNameEn(en).isPresent();
    }

    @Override
    public void saveItemStatus(OrderItemsStatus status) {
        _jpaOrderItemStatusRepo.save(status);
    }

    @Override
    public List<Orders> findOrdersByStatus(OrderStatus status) {
        return _jpaOrderRepo.findByStatusesContaining(status);
    }

    @Override
    public List<OrderItems> findOrderItemsByStatus(OrderItemsStatus status) {
        return _jpaOrderItemRepo.findByStatusesContaining(status);
    }

    @Override
    public long countOrderItemsStatuses() {
        return _jpaOrderItemStatusRepo.count();
    }

    @Override
    public long countStatuses() {
        return _jpaOrderStatusRepo.count();
    }

    @Override
    public long countItems() {
        return _jpaOrderItemRepo.count();
    }

    @Override
    public long count() {
        return _jpaOrderRepo.count();
    }

    @Override
    public Page<OrderItems> findAllItems(Pageable pageable) {
        return _jpaOrderItemRepo.findAll(pageable);
    }
}
