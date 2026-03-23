package com.example.restaurant.repository;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.domain.OrderSummaryDomain;
import com.example.restaurant.dto.domain.ReservationDomain;
import com.example.restaurant.dto.domain.TodayOrderSummaryDomain;
import com.example.restaurant.dto.request.ReservationDishRequest;
import com.example.restaurant.dto.response.TodayReservationDishResponse;
import com.example.restaurant.models.*;
import com.example.restaurant.models.lookup.Allergens;
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

    @Test
    void removeItemFromReservation_ShouldDecreaseQuantity_WhenPartialRemoval() {
        ReservationDishRequest request = new ReservationDishRequest();
        request.setDishToken(TestConstants.FAKE_DISH_TOKEN);
        request.setQuantity(1);
        request.setNote(null);

        Orders mockOrder = new Orders();
        mockOrder.setToken(TestConstants.FAKE_ORDER_TOKEN);
        mockOrder.setTotalPrice(150);

        Users mockWaiter = new Users();
        mockWaiter.setToken(TestConstants.FAKE_USER_TOKEN);
        mockOrder.setWaiter(mockWaiter);

        Dishes mockDish = new Dishes();
        mockDish.setToken(TestConstants.FAKE_DISH_TOKEN);

        OrderItems mockItem = new OrderItems();
        mockItem.setProduct(mockDish);
        mockItem.setQuantity(3);
        mockItem.setPriceAtTimeOfOrder(50);
        mockItem.setNote(null);

        when(_jpaOrderRepo.findByReservation_Token(TestConstants.FAKE_RESERVATION_TOKEN))
                .thenReturn(Optional.of(mockOrder));
        when(_jpaOrderItemRepo.findAllByOrder_Token(TestConstants.FAKE_ORDER_TOKEN))
                .thenReturn(List.of(mockItem));

        boolean result = _orderRepo.removeItemFromReservation(
                TestConstants.FAKE_USER_TOKEN,
                TestConstants.FAKE_RESERVATION_TOKEN,
                request);

        assertTrue(result);
        assertEquals(2, mockItem.getQuantity());
        assertEquals(100, mockOrder.getTotalPrice());

        verify(_jpaOrderItemRepo, times(1)).save(mockItem);
        verify(_jpaOrderRepo, times(1)).saveAndFlush(mockOrder);
    }

    @Test
    void removeItemFromReservation_ShouldDeleteRow_WhenFullRemoval() {
        String note = "Bez soli";
        ReservationDishRequest request = new ReservationDishRequest();
        request.setDishToken(TestConstants.FAKE_DISH_TOKEN);
        request.setQuantity(2);
        request.setNote(note);

        Orders mockOrder = new Orders();
        mockOrder.setToken(TestConstants.FAKE_ORDER_TOKEN);
        mockOrder.setTotalPrice(100);

        Users mockWaiter = new Users();
        mockWaiter.setToken(TestConstants.FAKE_USER_TOKEN);
        mockOrder.setWaiter(mockWaiter);

        Dishes mockDish = new Dishes();
        mockDish.setToken(TestConstants.FAKE_DISH_TOKEN);

        OrderItems mockItem = new OrderItems();
        mockItem.setProduct(mockDish);
        mockItem.setQuantity(2);
        mockItem.setPriceAtTimeOfOrder(50);
        mockItem.setNote(note);

        when(_jpaOrderRepo.findByReservation_Token(TestConstants.FAKE_RESERVATION_TOKEN))
                .thenReturn(Optional.of(mockOrder));
        when(_jpaOrderItemRepo.findAllByOrder_Token(TestConstants.FAKE_ORDER_TOKEN))
                .thenReturn(List.of(mockItem));

        boolean result = _orderRepo.removeItemFromReservation(
                TestConstants.FAKE_USER_TOKEN,
                TestConstants.FAKE_RESERVATION_TOKEN,
                request);

        assertTrue(result);
        assertEquals(0, mockOrder.getTotalPrice());

        verify(_jpaOrderItemRepo, times(1)).delete(mockItem);
        verify(_jpaOrderRepo, times(1)).saveAndFlush(mockOrder);
    }

    @Test
    void addItemFromReservation_ShouldIncreaseQuantityAndAddNewItem_WithCorrectPrices() {
        ReservationDishRequest existingDishRequest = new ReservationDishRequest();
        existingDishRequest.setDishToken(TestConstants.FAKE_DISH_TOKEN);
        existingDishRequest.setQuantity(2);
        existingDishRequest.setNote("bez życia");

        String newDishToken = "NEW_DISH_TOKEN";
        ReservationDishRequest newDishrequest = new ReservationDishRequest();
        newDishrequest.setDishToken(newDishToken);
        newDishrequest.setQuantity(1);
        newDishrequest.setNote("bez życia i piwa");

        List<ReservationDishRequest> requests = List.of(existingDishRequest, newDishrequest);

        Orders mockOrder = new Orders();
        mockOrder.setToken(TestConstants.FAKE_ORDER_TOKEN);
        mockOrder.setTotalPrice(100);

        Users mockWaiter = new Users();
        mockWaiter.setToken(TestConstants.FAKE_USER_TOKEN);
        mockOrder.setWaiter(mockWaiter);

        Dishes existingDish = new Dishes();
        existingDish.setToken(TestConstants.FAKE_DISH_TOKEN);
        existingDish.setPrice(50);

        Dishes newDish = new Dishes();
        newDish.setToken(newDishToken);
        newDish.setPrice(30);

        OrderItems existingItem = new OrderItems();
        existingItem.setProduct(existingDish);
        existingItem.setQuantity(1);
        existingItem.setPriceAtTimeOfOrder(40);
        existingItem.setNote("bez życia");

        when(_jpaOrderRepo.findByReservation_Token(TestConstants.FAKE_RESERVATION_TOKEN))
                .thenReturn(Optional.of(mockOrder));
        when(_jpaOrderItemRepo.findAllByOrder_Token(TestConstants.FAKE_ORDER_TOKEN))
                .thenReturn(List.of(existingItem));
        when(_jpaDishRepo.findAllByTokenIn(anyList())).thenReturn(List.of(existingDish, newDish));

        boolean result = _orderRepo.addItemFromReservation(
                TestConstants.FAKE_USER_TOKEN,
                TestConstants.FAKE_RESERVATION_TOKEN,
                requests
        );

        assertTrue(result);
        assertEquals(3, existingItem.getQuantity());
        assertEquals(210, mockOrder.getTotalPrice());
        verify(_jpaOrderRepo, times(1)).saveAndFlush(mockOrder);
    }

    @Test
    void addItemFromReservation_ShouldThrowException_WhenWaiterIsNotAssignedToOrder() {
        String assignedWaiterToken = "waiterA";
        String intruderWaiterToken = "waiterB";

        Users mockWaiter = new Users();
        mockWaiter.setToken(assignedWaiterToken);

        Orders mockOrder = new Orders();
        mockOrder.setWaiter(mockWaiter);

        when(_jpaOrderRepo.findByReservation_Token(TestConstants.FAKE_RESERVATION_TOKEN))
                .thenReturn(Optional.of(mockOrder));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> _orderRepo.addItemFromReservation(
                intruderWaiterToken,
                TestConstants.FAKE_RESERVATION_TOKEN,
                List.of()
        ));

        assertEquals("Order not found or you are not the assigned waiter", exception.getMessage());
        verify(_jpaOrderItemRepo, never()).save(any());
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

        boolean result = _orderRepo.assignWaiterToOrders(
                TestConstants.FAKE_RESERVATION_TOKEN,
                TestConstants.FAKE_USER_TOKEN
        );

        assertTrue(result);
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
    void isAbsent_ShouldReturnTrue_WhenOrderDoesNotExist() {
        when(_jpaOrderRepo.findByReservation_Token(TestConstants.FAKE_RESERVATION_TOKEN))
                .thenReturn(Optional.empty());

        boolean result = _orderRepo.isAbsent(TestConstants.FAKE_RESERVATION_TOKEN);

        assertTrue(result);
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

        boolean result = _orderRepo.isAbsent(TestConstants.FAKE_RESERVATION_TOKEN);

        assertTrue(result);
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
}
