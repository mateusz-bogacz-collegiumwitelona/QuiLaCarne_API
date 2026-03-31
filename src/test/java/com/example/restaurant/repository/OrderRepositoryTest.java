package com.example.restaurant.repository;

import com.example.restaurant.models.OrderItems;
import com.example.restaurant.models.Orders;
import com.example.restaurant.models.lookup.OrderStatus;
import com.example.restaurant.repository.interfaces.jpa.IJpaOrderItemStatusRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaOrderItemsRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaOrderRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaOrederStatusRepositry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderRepositoryTest {

    @Mock
    private IJpaOrderRepository _jpaOrderRepo;

    @Mock
    private IJpaOrderItemsRepository _jpaOrderItemRepo;

    @Mock
    private IJpaOrederStatusRepositry _jpaOrderStatusRepo;

    @Mock
    private IJpaOrderItemStatusRepository _jpaOrderItemStatusRepo;

    @InjectMocks
    private OrderRepository _orderRepo;

    @Test
    @DisplayName("Save ordrer with items: should save")
    void saveOrderWithItems_ShouldSaveOrderAndItems() {
        Orders mockOrder = new Orders();
        OrderItems mockItem = new OrderItems();
        List<OrderItems> items = List.of(mockItem);

        _orderRepo.saveOrderWithItems(mockOrder, items);

        verify(_jpaOrderRepo, times(1)).saveAndFlush(mockOrder);
        verify(_jpaOrderItemRepo, times(1)).saveAllAndFlush(items);
    }

    @Test
    @DisplayName("Find status by token: should return status if exist")
    void findStatusByToken_ShouldReturnStatus_WhenFound() {
        OrderStatus mockStatus = new OrderStatus();
        when(_jpaOrderStatusRepo.findByToken("PENDING"))
                .thenReturn(Optional.of(mockStatus));

        OrderStatus result = _orderRepo.findStatusByToken("PENDING");

        assertNotNull(result);
        verify(_jpaOrderStatusRepo, times(1)).findByToken("PENDING");
    }

    @Test
    @DisplayName("Find status by token: should throw exception if status doesn't exist")
    void findStatusByToken_ShouldThrowException_WhenStatusNotFound() {
        when(_jpaOrderStatusRepo.findByToken("PENDING"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                _orderRepo.findStatusByToken("PENDING")
        );

        assertEquals("Order status not found", exception.getMessage());
    }

    @Test
    @DisplayName("Save all items: shoould save all items")
    void saveAllItems_ShouldSaveAllItems() {
        List<OrderItems> items = List.of(new OrderItems(), new OrderItems());
        _orderRepo.saveAllItems(items);
        verify(_jpaOrderItemRepo, times(1)).saveAll(items);
    }

    @Test
    @DisplayName("Find status by token: should throw exception if status not found")
    void findItemStatusByToken_ShouldThrowException_WhenNotFound() {
        when(_jpaOrderItemStatusRepo.findByToken("INVALID")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> _orderRepo.findItemStatusByToken("INVALID"));
    }

    @Test
    @DisplayName("Find by reservation token: should call jpa")
    void findByReservationToken_ShouldCallJpa() {
        _orderRepo.findByReservationToken("RES_123");
        verify(_jpaOrderRepo).findByReservation_Token("RES_123");
    }
}
