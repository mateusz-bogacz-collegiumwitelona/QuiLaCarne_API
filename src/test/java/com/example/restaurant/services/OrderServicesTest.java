package com.example.restaurant.services;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.domain.OrderSummaryDomain;
import com.example.restaurant.dto.domain.ReservationDomain;
import com.example.restaurant.dto.domain.TodayOrderSummaryDomain;
import com.example.restaurant.dto.request.ReservationDishRequest;
import com.example.restaurant.dto.response.TodayReservationDishResponse;
import com.example.restaurant.models.*;
import com.example.restaurant.models.lookup.OrderItemsStatus;
import com.example.restaurant.models.lookup.OrderStatus;
import com.example.restaurant.repository.interfaces.*;
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

    @Mock
    private IUserRepository _userRepo;

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

    @Test
    void getOrderSummaryForReservation_ShouldReturnEmpty_WhenNoOrderExists() {
        when(_orderRepo.findByReservationToken(TestConstants.FAKE_RESERVATION_TOKEN))
                .thenReturn(Optional.empty());

        OrderSummaryDomain result = _orderServices
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

        when(_orderRepo.findByReservationToken(TestConstants.FAKE_RESERVATION_TOKEN))
                .thenReturn(Optional.of(mockOrder));
        when(_orderRepo.findItemsByOrderToken(TestConstants.FAKE_ORDER_TOKEN))
                .thenReturn(List.of(mockItem));

        OrderSummaryDomain result = _orderServices.getOrderSummaryForReservation(TestConstants.FAKE_RESERVATION_TOKEN);

        assertNotNull(result);
        assertEquals(150, result.totalPrice());
        assertEquals(1, result.dishes().size());
        assertEquals("Pizza", result.dishes().get(0).getDishName());
        assertEquals(50, result.dishes().get(0).getPrice());
        assertEquals(3, result.dishes().get(0).getQuantity());
    }

    @Test
    void todayOrderDetails_ShouldReturnEmptyDomain_WhenOrderNotFound() {
        when(_orderRepo.findByReservationToken("fake-res-token"))
                .thenReturn(Optional.empty());

        TodayOrderSummaryDomain result = _orderServices.todayOrderDetails("fake-res-token", "pl");

        assertEquals(0, result.totalPrice());
        assertTrue(result.dishes().isEmpty());

        verify(_orderRepo, never()).findItemsByOrderToken(anyString());
    }

    @Test
    void todayOrderDetails_ShouldReturnMappedData() {
        Orders mockOrder = new Orders();
        mockOrder.setToken("fake-order-token");
        mockOrder.setTotalPrice(120);

        com.example.restaurant.models.lookup.Allergens gluten = new com.example.restaurant.models.lookup.Allergens();
        gluten.setNamePl("Gluten");

        com.example.restaurant.models.lookup.Allergens lactose = new com.example.restaurant.models.lookup.Allergens();
        lactose.setNamePl("Laktoza");

        Ingredients pasta = new Ingredients();
        pasta.setNamePl("Makaron");
        pasta.setAllergens(Set.of(gluten));

        Ingredients cheese = new Ingredients();
        cheese.setNamePl("Ser");
        cheese.setAllergens(Set.of(lactose));

        Dishes mockDish = new Dishes();
        mockDish.setToken("dish-token");
        mockDish.setName("Spaghetti");
        mockDish.setPrice(60);
        mockDish.setIngredients(Set.of(pasta, cheese));

        OrderItems mockItem = new OrderItems();
        mockItem.setProduct(mockDish);
        mockItem.setQuantity(2);
        mockItem.setPriceAtTimeOfOrder(60);
        mockItem.setNote("Bez soli");

        when(_orderRepo.findByReservationToken("fake-res-token"))
                .thenReturn(Optional.of(mockOrder));
        when(_orderRepo.findItemsByOrderToken("fake-order-token"))
                .thenReturn(List.of(mockItem));

        TodayOrderSummaryDomain result = _orderServices.todayOrderDetails("fake-res-token", "pl");

        assertEquals(120, result.totalPrice());
        assertEquals(1, result.dishes().size());

        TodayReservationDishResponse dishRes = result.dishes().get(0);
        assertEquals("Spaghetti", dishRes.getDishName());
        assertEquals(60, dishRes.getPrice());
        assertEquals(2, dishRes.getQuantity());
        assertEquals("Bez soli", dishRes.getNote());

        assertEquals(2, dishRes.getAllergens().size());
        assertTrue(dishRes.getAllergens().contains("Gluten"));
        assertTrue(dishRes.getAllergens().contains("Laktoza"));
    }

    @Test
    void removeItemFromReservation_ShouldDecreaseQuantityAndCreateCancelled_WhenPartialRemoval() {
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
        mockItem.setStatuses(new HashSet<>());

        com.example.restaurant.models.lookup.OrderItemsStatus cancelledStatus = new com.example.restaurant.models.lookup.OrderItemsStatus();
        cancelledStatus.setToken("CANCELLED");

        when(_orderRepo.findByReservationToken(TestConstants.FAKE_RESERVATION_TOKEN))
                .thenReturn(Optional.of(mockOrder));
        when(_orderRepo.findItemsByOrderToken(TestConstants.FAKE_ORDER_TOKEN))
                .thenReturn(List.of(mockItem));
        when(_orderRepo.findItemStatusByToken("CANCELLED"))
                .thenReturn(cancelledStatus);

        _orderServices.removeItemFromReservation(
                TestConstants.FAKE_USER_TOKEN,
                TestConstants.FAKE_RESERVATION_TOKEN,
                request
        );

        assertEquals(2, mockItem.getQuantity());
        assertEquals(100, mockOrder.getTotalPrice());

        verify(_orderRepo, times(2)).saveItem(any(OrderItems.class)); // Zapis oryginalnego i nowego(anulowanego) rekordu
        verify(_orderRepo, times(1)).save(mockOrder);
    }

    @Test
    void removeItemFromReservation_ShouldSetStatusToCancelled_WhenFullRemoval() {
        // Arrange
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
        mockItem.setStatuses(new HashSet<>());

        com.example.restaurant.models.lookup.OrderItemsStatus cancelledStatus = new com.example.restaurant.models.lookup.OrderItemsStatus();
        cancelledStatus.setToken("CANCELLED");

        when(_orderRepo.findByReservationToken(TestConstants.FAKE_RESERVATION_TOKEN))
                .thenReturn(Optional.of(mockOrder));
        when(_orderRepo.findItemsByOrderToken(TestConstants.FAKE_ORDER_TOKEN))
                .thenReturn(List.of(mockItem));
        when(_orderRepo.findItemStatusByToken("CANCELLED"))
                .thenReturn(cancelledStatus);

        _orderServices.removeItemFromReservation(
                TestConstants.FAKE_USER_TOKEN,
                TestConstants.FAKE_RESERVATION_TOKEN,
                request
        );

        assertEquals(0, mockOrder.getTotalPrice());
        assertTrue(mockItem.getStatuses().contains(cancelledStatus));

        verify(_orderRepo, times(1)).saveItem(mockItem);
        verify(_orderRepo, times(1)).save(mockOrder);
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
        existingItem.setStatuses(new HashSet<>());

        when(_orderRepo.findByReservationToken(TestConstants.FAKE_RESERVATION_TOKEN))
                .thenReturn(Optional.of(mockOrder));
        when(_orderRepo.findItemsByOrderToken(TestConstants.FAKE_ORDER_TOKEN))
                .thenReturn(List.of(existingItem));
        when(_dishRepo.listForOrder(anyList())).thenReturn(List.of(existingDish, newDish));

        _orderServices.addItemFromReservation(
                TestConstants.FAKE_USER_TOKEN,
                TestConstants.FAKE_RESERVATION_TOKEN,
                requests
        );

        assertEquals(3, existingItem.getQuantity());
        assertEquals(210, mockOrder.getTotalPrice());

        verify(_orderRepo, times(2)).saveItem(any(OrderItems.class));
        verify(_orderRepo, times(1)).save(mockOrder);
    }

    @Test
    void addItemFromReservation_ShouldThrowException_WhenWaiterIsNotAssignedToOrder() {
        String assignedWaiterToken = "waiterA";
        String intruderWaiterToken = "waiterB";

        Users mockWaiter = new Users();
        mockWaiter.setToken(assignedWaiterToken);

        Orders mockOrder = new Orders();
        mockOrder.setWaiter(mockWaiter);

        when(_orderRepo.findByReservationToken(TestConstants.FAKE_RESERVATION_TOKEN))
                .thenReturn(Optional.of(mockOrder));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> _orderServices.addItemFromReservation(
                intruderWaiterToken,
                TestConstants.FAKE_RESERVATION_TOKEN,
                List.of()
        ));

        assertEquals("Order not found or you are not the assigned waiter", exception.getMessage());
        verify(_orderRepo, never()).save(any());
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

        when(_orderRepo.findByReservationToken(TestConstants.FAKE_RESERVATION_TOKEN)).thenReturn(Optional.of(mockOrder));
        when(_userRepo.findByToken(TestConstants.FAKE_USER_TOKEN)).thenReturn(mockWaiter);

        when(_orderRepo.findStatusByToken(inProgressStatus)).thenReturn(inProgressOrder);
        when(_orderRepo.findItemStatusByToken(inProgressStatus)).thenReturn(inProgressItem);

        when(_orderRepo.findItemsByOrderToken(mockOrder.getToken()))
                .thenReturn(List.of(pendingDish, emptyStatusDish, cancelledDish));

        _orderServices.assignWaiterToOrders(
                TestConstants.FAKE_RESERVATION_TOKEN,
                TestConstants.FAKE_USER_TOKEN
        );

        assertEquals(mockWaiter, mockOrder.getWaiter());
        assertTrue(mockOrder.getStatuses().contains(inProgressOrder));

        assertTrue(pendingDish.getStatuses().contains(inProgressItem));
        assertTrue(emptyStatusDish.getStatuses().contains(inProgressItem));

        assertFalse(cancelledDish.getStatuses().contains(inProgressItem));
        assertTrue(cancelledDish.getStatuses().contains(cancelledItemStatus));

        verify(_orderRepo, times(1)).saveAllItems(anyList());
        verify(_orderRepo, times(1)).save(mockOrder);
    }

    @Test
    void isAbsent_ShouldDoNothing_WhenOrderDoesNotExist() {
        when(_orderRepo.findByReservationToken(TestConstants.FAKE_RESERVATION_TOKEN))
                .thenReturn(Optional.empty());

        _orderServices.isAbsent(TestConstants.FAKE_RESERVATION_TOKEN);

        verify(_orderRepo, never()).findStatusByToken(anyString());
        verify(_orderRepo, never()).save(any());
    }

    @Test
    void isAbsent_ShouldCancelOrderAndItems_WhenOrderExists() {
        Orders mockOrder = new Orders();
        mockOrder.setToken(TestConstants.FAKE_ORDER_TOKEN);

        OrderItems mockItem1 = new OrderItems();
        mockItem1.setStatuses(new HashSet<>());
        OrderItems mockItem2 = new OrderItems();
        mockItem2.setStatuses(new HashSet<>());
        List<OrderItems> orderItems = List.of(mockItem1, mockItem2);

        OrderStatus cancelledStatus = new OrderStatus();
        cancelledStatus.setToken("CANCELLED");

        OrderItemsStatus cancelledItemStatus = new OrderItemsStatus();
        cancelledItemStatus.setToken("CANCELLED");

        when(_orderRepo.findByReservationToken(TestConstants.FAKE_RESERVATION_TOKEN))
                .thenReturn(Optional.of(mockOrder));

        when(_orderRepo.findStatusByToken("CANCELLED"))
                .thenReturn(cancelledStatus);
        when(_orderRepo.findItemStatusByToken("CANCELLED"))
                .thenReturn(cancelledItemStatus);
        when(_orderRepo.findItemsByOrderToken(mockOrder.getToken()))
                .thenReturn(orderItems);

        _orderServices.isAbsent(TestConstants.FAKE_RESERVATION_TOKEN);

        assertTrue(mockOrder.getStatuses().contains(cancelledStatus));
        assertTrue(mockItem1.getStatuses().contains(cancelledItemStatus));
        assertTrue(mockItem2.getStatuses().contains(cancelledItemStatus));

        verify(_orderRepo, times(1)).saveAllItems(orderItems);
        verify(_orderRepo, times(1)).save(mockOrder);
    }
}