package com.example.restaurant.repository;

import com.example.restaurant.TestConstants;
import com.example.restaurant.models.OrderItems;
import com.example.restaurant.models.Orders;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.lookup.OrderItemsStatus;
import com.example.restaurant.models.lookup.OrderStatus;
import com.example.restaurant.repository.interfaces.jpa.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderRepositoryTest {
    @Mock
    private IJpaDishRepository _jpaDishRepo;
    @Mock
    private IJpaOrderRepository _jpaOrderRepo;
    @Mock
    private IJpaOrderItemsRepository _jpaOrderItemRepo;
    @Mock
    private IJpaOrederStatusRepositry _jpaOrderStatusRepo;
    @Mock
    private IJpaReservationsRepository _jpaReservationsRepo;
    @Mock
    private IJpaTableRepository _jpaTableRepo;
    @Mock
    private IJpaOrderItemStatusRepository _jpaOrderItemStatusRepo;
    @Mock
    private IJpaUserRepository _jpaUserRepo;

    @InjectMocks
    private OrderRepository _orderRepo;

    @Test
    void saveOrderWithItems_ShouldSaveOrderAndItems() {
        Orders mockOrder = new Orders();
        OrderItems mockItem = new OrderItems();
        List<OrderItems> items = List.of(mockItem);

        _orderRepo.saveOrderWithItems(mockOrder, items);

        verify(_jpaOrderRepo, times(1)).saveAndFlush(mockOrder);
        verify(_jpaOrderItemRepo, times(1)).saveAllAndFlush(items);
    }


    @Test
    void assignWaiterToOrders_ShouldAssignWaiterAndOnlyChangePendingItems() {
        String inProgressStatus = "IN_PROGRESS";

        Orders mockOrder = new Orders();
        mockOrder.setToken(TestConstants.FAKE_ORDER_TOKEN);

        Users mockWaiter = new Users();
        mockWaiter.setToken(TestConstants.FAKE_USER_TOKEN);

        OrderStatus inProgressOrder = new OrderStatus();
        inProgressOrder.setToken(inProgressStatus);

        OrderItemsStatus inProgressItem = new OrderItemsStatus();
        inProgressItem.setToken(inProgressStatus);

        OrderItemsStatus pendingItemStatus = new OrderItemsStatus();
        pendingItemStatus.setToken("PENDING");

        OrderItemsStatus cancelledItemStatus = new OrderItemsStatus();
        cancelledItemStatus.setToken("CANCELLED");

        OrderItems pendingDish = new OrderItems();
        pendingDish.setStatuses(new HashSet<>(Set.of(pendingItemStatus)));

        OrderItems emptyStatusDish = new OrderItems();
        emptyStatusDish.setStatuses(new HashSet<>());

        OrderItems cancelledDish = new OrderItems();
        cancelledDish.setStatuses(new HashSet<>(Set.of(cancelledItemStatus)));

        when(_jpaOrderRepo.findByReservation_Token(TestConstants.FAKE_RESERVATION_TOKEN)).thenReturn(Optional.of(mockOrder));
        when(_jpaUserRepo.findByToken(TestConstants.FAKE_USER_TOKEN)).thenReturn(Optional.of(mockWaiter));
        when(_jpaOrderStatusRepo.findByToken(inProgressStatus)).thenReturn(Optional.of(inProgressOrder));
        when(_jpaOrderItemStatusRepo.findByToken(inProgressStatus)).thenReturn(Optional.of(inProgressItem));

        when(_jpaOrderItemRepo.findAllByOrder_Token(TestConstants.FAKE_ORDER_TOKEN))
                .thenReturn(List.of(pendingDish, emptyStatusDish, cancelledDish));

        _orderRepo.assignWaiterToOrders(
                TestConstants.FAKE_RESERVATION_TOKEN,
                TestConstants.FAKE_USER_TOKEN
        );

        assertEquals(mockWaiter, mockOrder.getWaiter());
        assertTrue(mockOrder.getStatuses().contains(inProgressOrder));

        assertTrue(pendingDish.getStatuses().contains(inProgressItem));
        assertTrue(emptyStatusDish.getStatuses().contains(inProgressItem));

        assertFalse(cancelledDish.getStatuses().contains(inProgressItem));
        assertTrue(cancelledDish.getStatuses().contains(cancelledItemStatus));

        verify(_jpaOrderItemRepo, times(1)).saveAll(anyList());
        verify(_jpaOrderRepo, times(1)).saveAndFlush(mockOrder);
    }

    @Test
    void isAbsent_ShouldDoNothing_WhenOrderDoesNotExist() {
        when(_jpaOrderRepo.findByReservation_Token(TestConstants.FAKE_RESERVATION_TOKEN))
                .thenReturn(Optional.empty());

        _orderRepo.isAbsent(TestConstants.FAKE_RESERVATION_TOKEN);

        verify(_jpaOrderStatusRepo, never()).findByToken(anyString());
        verify(_jpaOrderRepo, never()).saveAndFlush(any());
    }

    @Test
    void isAbsent_ShouldCancelOrderAndItems_WhenOrderExists() {
        Orders mockOrder = new Orders();
        mockOrder.setToken(TestConstants.FAKE_ORDER_TOKEN);

        OrderItems mockItem1 = new OrderItems();
        OrderItems mockItem2 = new OrderItems();
        List<OrderItems> orderItems = List.of(mockItem1, mockItem2);

        OrderStatus cancelledStatus = new OrderStatus();
        cancelledStatus.setToken("CANCELLED");

        OrderItemsStatus cancelledItemStatus = new OrderItemsStatus();
        cancelledItemStatus.setToken("CANCELLED");

        when(_jpaOrderRepo.findByReservation_Token(TestConstants.FAKE_RESERVATION_TOKEN))
                .thenReturn(Optional.of(mockOrder));
        when(_jpaOrderStatusRepo.findByToken("CANCELLED"))
                .thenReturn(Optional.of(cancelledStatus));
        when(_jpaOrderItemStatusRepo.findByToken("CANCELLED"))
                .thenReturn(Optional.of(cancelledItemStatus));
        when(_jpaOrderItemRepo.findAllByOrder_Token(mockOrder.getToken()))
                .thenReturn(orderItems);

        _orderRepo.isAbsent(TestConstants.FAKE_RESERVATION_TOKEN);

        assertTrue(mockOrder.getStatuses().contains(cancelledStatus));
        assertTrue(mockItem1.getStatuses().contains(cancelledItemStatus));
        assertTrue(mockItem2.getStatuses().contains(cancelledItemStatus));

        verify(_jpaOrderItemRepo, times(1)).saveAll(orderItems);
        verify(_jpaOrderRepo, times(1)).saveAndFlush(mockOrder);
    }

    @Test
    void isAbsent_ShouldThrowException_WhenOrderStatusNotFound() {
        Orders mockOrder = new Orders();
        when(_jpaOrderRepo.findByReservation_Token(TestConstants.FAKE_RESERVATION_TOKEN))
                .thenReturn(Optional.of(mockOrder));
        when(_jpaOrderStatusRepo.findByToken("CANCELLED"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                _orderRepo.isAbsent(TestConstants.FAKE_RESERVATION_TOKEN)
        );
        assertEquals("Order status not found", exception.getMessage());
    }

    @Test
    void findStatusByToken_ShouldReturnStatus_WhenFound() {
        OrderStatus mockStatus = new OrderStatus();
        when(_jpaOrderStatusRepo.findByToken("PENDING"))
                .thenReturn(Optional.of(mockStatus));

        OrderStatus result = _orderRepo.findStatusByToken("PENDING");

        assertNotNull(result);
        verify(_jpaOrderStatusRepo, times(1)).findByToken("PENDING");
    }

    @Test
    void findStatusByToken_ShouldThrowException_WhenStatusNotFound() {
        when(_jpaOrderStatusRepo.findByToken("PENDING"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                _orderRepo.findStatusByToken("PENDING")
        );

        assertEquals("Order status not found", exception.getMessage());
    }
}
