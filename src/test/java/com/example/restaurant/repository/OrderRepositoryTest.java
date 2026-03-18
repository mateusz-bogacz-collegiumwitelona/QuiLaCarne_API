package com.example.restaurant.repository;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.domain.OrderSummaryDomain;
import com.example.restaurant.dto.domain.ReservationDomain;
import com.example.restaurant.dto.domain.TodayOrderSummaryDomain;
import com.example.restaurant.dto.request.ReservationDishRequest;
import com.example.restaurant.dto.response.TodayReservationDishResponse;
import com.example.restaurant.models.*;
import com.example.restaurant.models.lookup.Allergens;
import com.example.restaurant.models.lookup.OrderStatus;
import com.example.restaurant.repository.interfaces.jpa.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        assertEquals(100, result.totalPrice());
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

    @Test
    void getOrderSummaryForReservation_ShouldReturnEmpty_WhenNoOrderExists() {
        when(_jpaOrderRepo.findByReservation_Token(TestConstants.FAKE_RESERVATION_TOKEN))
                .thenReturn(Optional.empty());

        OrderSummaryDomain result = _orderRepo
                .getOrderSummaryForReservation(TestConstants.FAKE_RESERVATION_TOKEN);

        assertNotNull(result);
        assertEquals(0, result.totalPrice());
        assertTrue(result.dishes().isEmpty());
    }

    @Test
    void getOrderSummaryForReservation_ShouldReturnMappedDishes_WhenOrderExists() {
        Orders mockOrder = new Orders();
        mockOrder.setToken(TestConstants.FAKE_ORDER_TOKEN);
        mockOrder.setTotalPrice(150);

        Dishes mockDish = new Dishes();
        mockDish.setName("Pizza");

        OrderItems mockItem = new OrderItems();
        mockItem.setProduct(mockDish);
        mockItem.setQuantity(3);
        mockItem.setPriceAtTimeOfOrder(50);

        when(_jpaOrderRepo.findByReservation_Token(TestConstants.FAKE_RESERVATION_TOKEN))
                .thenReturn(Optional.of(mockOrder));
        when(_jpaOrderItemRepo.findAllByOrder_Token(TestConstants.FAKE_ORDER_TOKEN))
                .thenReturn(List.of(mockItem));

        OrderSummaryDomain result = _orderRepo.getOrderSummaryForReservation(TestConstants.FAKE_RESERVATION_TOKEN);

        assertNotNull(result);
        assertEquals(150, result.totalPrice());
        assertEquals(1, result.dishes().size());
        assertEquals("Pizza", result.dishes().get(0).getDishName());
        assertEquals(50, result.dishes().get(0).getPrice());
        assertEquals(3, result.dishes().get(0).getQuantity());
    }

    @Test
    void todayOrderDetails_ShouldReturnEmptyDomain_WhenOrderNotFound() {
        when(_jpaOrderRepo.findByReservation_Token("fake-res-token"))
                .thenReturn(Optional.empty());

        TodayOrderSummaryDomain result = _orderRepo.todayOrderDetails("fake-res-token", "pl");

        assertEquals(0, result.totalPrice());
        assertTrue(result.dishes().isEmpty());

        verify(_jpaOrderItemRepo, never()).findAllByOrder_Token(anyString());
    }

    @Test
    void todayOrderDetails_ShouldReturnMappedData() {
        Orders mockOrder = new Orders();
        mockOrder.setToken("fake-order-token");
        mockOrder.setTotalPrice(120);

        Allergens gluten = new Allergens();
        gluten.setNamePl("Gluten");
        gluten.setNameEn("Gluten EN");

        Allergens lactose = new Allergens();
        lactose.setNamePl("Laktoza");
        lactose.setNameEn("Lactose EN");

        Ingredients pasta = new Ingredients();
        pasta.setNamePl("Makaron");
        pasta.setNameEn("Pasta");
        pasta.setAllergens(Set.of(gluten, lactose));

        Ingredients cheese = new Ingredients();
        cheese.setNamePl("Ser");
        cheese.setNameEn("Cheese");
        cheese.setAllergens(Set.of(lactose));

        Dishes mockDish = new Dishes();
        mockDish.setToken("dish-token");
        mockDish.setName("Spaghetti");
        mockDish.setPrice(60);
        mockDish.setIngredients(Set.of(pasta, cheese));

        OrderItems mockItem = new OrderItems();
        mockItem.setProduct(mockDish);
        mockItem.setQuantity(2);
        mockItem.setNote("Bez soli");

        when(_jpaOrderRepo.findByReservation_Token("fake-res-token"))
                .thenReturn(Optional.of(mockOrder));
        when(_jpaOrderItemRepo.findAllByOrder_Token("fake-order-token"))
                .thenReturn(List.of(mockItem));

        TodayOrderSummaryDomain result = _orderRepo.todayOrderDetails("fake-res-token", "pl");

        assertEquals(120, result.totalPrice());
        assertEquals(1, result.dishes().size());

        TodayReservationDishResponse dishRes = result.dishes().get(0);
        assertEquals("dish-token", dishRes.getDishToken());
        assertEquals("Spaghetti", dishRes.getDishName());
        assertEquals(60, dishRes.getPrice());
        assertEquals(2, dishRes.getQuantity());
        assertEquals("Bez soli", dishRes.getNote());

        assertEquals(2, dishRes.getIngredient().size());
        assertTrue(dishRes.getIngredient().contains("Makaron"));
        assertTrue(dishRes.getIngredient().contains("Ser"));

        assertEquals(2, dishRes.getAllergens().size());
        assertTrue(dishRes.getAllergens().contains("Gluten"));
        assertTrue(dishRes.getAllergens().contains("Laktoza"));
    }
}
