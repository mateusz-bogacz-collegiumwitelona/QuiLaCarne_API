package com.example.restaurant.repository;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.domain.ReservationDomain;
import com.example.restaurant.dto.request.ReservationDishRequest;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.models.Orders;
import com.example.restaurant.models.Reservations;
import com.example.restaurant.models.RestaurantTables;
import com.example.restaurant.models.lookup.OrderStatus;
import com.example.restaurant.repository.interfaces.jpa.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderRepositoryTest {
    @Mock private IJpaDishRepository _jpaDishRepo;
    @Mock private IJpaOrderRepository _jpaOrderRepo;
    @Mock private IJpaOrderItemsRepository _jpaOrderItemRepo;
    @Mock private IJpaOrederStatusRepositry _jpaOrderStatusRepo;
    @Mock private IJpaReservationsRepository _jpaReservationsRepo;
    @Mock private IJpaTableRepository _jpaTableRepo;

    @InjectMocks
    private OrderRepository _orderRepo;

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

        when(_jpaReservationsRepo.findByToken(TestConstants.FAKE_RESERVATION_TOKEN)).thenReturn(Optional.of(reservation));
        when(_jpaTableRepo.findByToken(TestConstants.FAKE_TABLE_TOKEN)).thenReturn(table);
        when(_jpaDishRepo.findAllByTokenIn(List.of(TestConstants.FAKE_DISH_TOKEN))).thenReturn(List.of(dishEntity));
        when(_jpaOrderStatusRepo.findByToken("PENDING")).thenReturn(Optional.of(status));

        ReservationDomain result = _orderRepo.createOrderForReservation(
                TestConstants.FAKE_RESERVATION_TOKEN,
                TestConstants.FAKE_TABLE_TOKEN,
                List.of(dishReq));

        assertNotNull(result);
        assertEquals(100, result.totalPrice()); // 2x Pizza za 50
        assertEquals(1, result.dishes().size());
        assertEquals("Pizza", result.dishes().get(0).dishName());
        assertEquals(50, result.dishes().get(0).price());

        verify(_jpaOrderRepo, times(1)).saveAndFlush(any(Orders.class));
        verify(_jpaOrderItemRepo, times(1)).saveAllAndFlush(anyList());
    }

    @Test
    void createOrderForReservation_ShouldThrowException_WhenDishNotFound() {
        ReservationDishRequest dishReq = new ReservationDishRequest();
        dishReq.setDishToken(TestConstants.FAKE_DISH_TOKEN);

        when(_jpaReservationsRepo.findByToken(anyString())).thenReturn(Optional.of(new Reservations()));
        when(_jpaOrderStatusRepo.findByToken("PENDING")).thenReturn(Optional.of(new OrderStatus()));
        when(_jpaDishRepo.findAllByTokenIn(anyList())).thenReturn(List.of());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                _orderRepo.createOrderForReservation(
                        TestConstants.FAKE_RESERVATION_TOKEN,
                        TestConstants.FAKE_TABLE_TOKEN,
                        List.of(dishReq)
                )
        );
        assertEquals("Dish not found", exception.getMessage());
    }
}
