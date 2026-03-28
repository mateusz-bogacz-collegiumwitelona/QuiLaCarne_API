package com.example.restaurant.services;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.domain.ReservationDomain;
import com.example.restaurant.dto.request.ReservationDishRequest;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.models.Orders;
import com.example.restaurant.models.Reservations;
import com.example.restaurant.models.RestaurantTables;
import com.example.restaurant.models.lookup.OrderStatus;
import com.example.restaurant.repository.interfaces.IDishRepository;
import com.example.restaurant.repository.interfaces.IOrderRepository;
import com.example.restaurant.repository.interfaces.IReservationRepository;
import com.example.restaurant.repository.interfaces.ITableRespository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServicesTest {
    @Mock
    private IOrderRepository _orderRepo;
    @Mock
    private IDishRepository _dishRepo;
    @Mock
    private IReservationRepository _reservationRepo;
    @Mock
    private ITableRespository _tableRepo;

    @InjectMocks
    private OrderServices _orderServices;

    @Test
    void createOrderForReservation_ShouldCalculatePriceAndReturnDomain_WhenSuccessful() {
        ReservationDishRequest dishReq = new ReservationDishRequest();
        dishReq.setDishToken(TestConstants.FAKE_DISH_TOKEN);
        dishReq.setQuantity(2);
        dishReq.setNote("No onion");

        Reservations reservation = new Reservations();
        RestaurantTables table = new RestaurantTables();
        OrderStatus status = new OrderStatus();

        Dishes dishEntity = new Dishes();
        dishEntity.setToken(TestConstants.FAKE_DISH_TOKEN);
        dishEntity.setName("Pizza");
        dishEntity.setPrice(50);

        when(_dishRepo.listForOrder(List.of(TestConstants.FAKE_DISH_TOKEN))).thenReturn(List.of(dishEntity));
        when(_reservationRepo.findByToken(TestConstants.FAKE_RESERVATION_TOKEN)).thenReturn(reservation);
        when(_tableRepo.findByToken(TestConstants.FAKE_TABLE_TOKEN)).thenReturn(table);
        when(_orderRepo.findStatusByToken("PENDING")).thenReturn(status);

        ReservationDomain result = _orderServices.createOrderForReservation(
                TestConstants.FAKE_RESERVATION_TOKEN,
                TestConstants.FAKE_TABLE_TOKEN,
                List.of(dishReq));

        assertNotNull(result);
        assertEquals(100, result.totalPrice());
        assertEquals(1, result.dishes().size());
        assertEquals("Pizza", result.dishes().get(0).dishName());
        assertEquals(50, result.dishes().get(0).price());

        verify(_orderRepo, times(1)).saveOrderWithItems(any(Orders.class), anyList());
    }

    @Test
    void createOrderForReservation_ShouldReturnEmptyDomain_WhenNoDishesRequested() {
        ReservationDomain result = _orderServices.createOrderForReservation(
                TestConstants.FAKE_RESERVATION_TOKEN,
                TestConstants.FAKE_TABLE_TOKEN,
                List.of());

        assertNotNull(result);
        assertEquals(0, result.totalPrice());
        assertTrue(result.dishes().isEmpty());

        verify(_orderRepo, never()).saveOrderWithItems(any(), any());
    }

    @Test
    void createOrderForReservation_ShouldThrowException_WhenDishNotFoundInMap() {
        ReservationDishRequest dishReq = new ReservationDishRequest();
        dishReq.setDishToken(TestConstants.FAKE_DISH_TOKEN);
        dishReq.setQuantity(2);

        when(_reservationRepo.findByToken(TestConstants.FAKE_RESERVATION_TOKEN)).thenReturn(new Reservations());
        when(_tableRepo.findByToken(TestConstants.FAKE_TABLE_TOKEN)).thenReturn(new RestaurantTables());
        when(_orderRepo.findStatusByToken("PENDING")).thenReturn(new OrderStatus());

        when(_dishRepo.listForOrder(List.of(TestConstants.FAKE_DISH_TOKEN))).thenReturn(List.of());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                _orderServices.createOrderForReservation(
                        TestConstants.FAKE_RESERVATION_TOKEN,
                        TestConstants.FAKE_TABLE_TOKEN,
                        List.of(dishReq)
                )
        );

        assertTrue(exception.getMessage().contains("Dish not found"));
        verify(_orderRepo, never()).saveOrderWithItems(any(), any());
    }
}