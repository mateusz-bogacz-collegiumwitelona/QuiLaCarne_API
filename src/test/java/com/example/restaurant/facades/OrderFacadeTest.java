package com.example.restaurant.facades;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.domain.OrderSummaryDomain;
import com.example.restaurant.dto.domain.ReservationDomain;
import com.example.restaurant.dto.domain.TodayOrderSummaryDomain;
import com.example.restaurant.dto.request.AddEntityRequest;
import com.example.restaurant.dto.request.ReservationDishRequest;
import com.example.restaurant.dto.response.DictionaryResponse;
import com.example.restaurant.dto.response.TodayReservationDishResponse;
import com.example.restaurant.dto.sync.SyncDictionaryResponse;
import com.example.restaurant.dto.sync.SyncOrderItemResponse;
import com.example.restaurant.dto.sync.SyncOrderResponse;
import com.example.restaurant.enums.WebSocketEventType;
import com.example.restaurant.fasade.OrderFacade;
import com.example.restaurant.mappers.SyncMapper;
import com.example.restaurant.models.*;
import com.example.restaurant.models.lookup.Allergens;
import com.example.restaurant.models.lookup.OrderItemsStatus;
import com.example.restaurant.models.lookup.OrderStatus;
import com.example.restaurant.repository.interfaces.*;
import com.example.restaurant.services.NotificationServices;
import com.example.restaurant.services.order.OrderDictionaryService;
import com.example.restaurant.services.order.OrderQueryService;
import com.example.restaurant.services.order.OrderSyncPublisher;
import com.example.restaurant.services.order.OrderWorkflowService;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;

@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {
  @Mock private IOrderRepository _orderRepo;

  @Mock private IDishRepository _dishRepo;

  @Mock private IReservationRepository _reservationRepo;

  @Mock private ITableRespository _tableRepo;

  @Mock private IUserRepository _userRepo;

  @Mock private NotificationServices _notification;

  @InjectMocks private OrderFacade _orderFacade;

  @Spy private SyncMapper _syncMapper = Mappers.getMapper(SyncMapper.class);

  @AfterEach
  void tearDown() {
    LocaleContextHolder.resetLocaleContext();
  }

  @BeforeEach
  void setUp() {
    OrderSyncPublisher syncPublisher = new OrderSyncPublisher(_notification, _syncMapper);

    OrderWorkflowService workflow =
        new OrderWorkflowService(
            _orderRepo, syncPublisher, _dishRepo, _reservationRepo, _tableRepo, _userRepo);
    OrderQueryService query = new OrderQueryService(_orderRepo);
    OrderDictionaryService dictionary = new OrderDictionaryService(_orderRepo, syncPublisher);

    this._orderFacade = new OrderFacade(workflow, query, dictionary);
  }

  @Test
  @DisplayName(
      "Create order for reservation: Should Calculate price and return domain when success")
  void createOrderForReservation_ShouldCalculatePriceAndReturnDomain_WhenSuccessful() {
    ReservationDishRequest dishReq = new ReservationDishRequest();
    dishReq.setDishToken(TestConstants.FAKE_DISH_TOKEN);
    dishReq.setQuantity(2);
    dishReq.setNote("No onion");

    Reservations reservation = new Reservations();
    reservation.setToken(TestConstants.FAKE_RESERVATION_TOKEN);
    RestaurantTables table = new RestaurantTables();
    table.setToken(TestConstants.FAKE_TABLE_TOKEN);
    OrderStatus status = new OrderStatus();
    status.setToken("PENDING");

    OrderItemsStatus itemPendingStatus = new OrderItemsStatus();
    itemPendingStatus.setToken("PENDING");

    Dishes dishEntity = new Dishes();
    dishEntity.setToken(TestConstants.FAKE_DISH_TOKEN);
    dishEntity.setName("Pizza");
    dishEntity.setPrice(50);

    when(_dishRepo.listForOrder(List.of(TestConstants.FAKE_DISH_TOKEN)))
        .thenReturn(List.of(dishEntity));
    when(_reservationRepo.findByToken(TestConstants.FAKE_RESERVATION_TOKEN))
        .thenReturn(Optional.of(reservation));
    when(_tableRepo.findByToken(TestConstants.FAKE_TABLE_TOKEN)).thenReturn(table);
    when(_orderRepo.findStatusByToken("PENDING")).thenReturn(status);
    when(_orderRepo.findItemStatusByToken("PENDING")).thenReturn(itemPendingStatus);

    doAnswer(
            invocation -> {
              Orders o = invocation.getArgument(0);
              o.setToken("NEW_ORDER_TOKEN");
              return null;
            })
        .when(_orderRepo)
        .saveOrderWithItems(any(Orders.class), anyList());

    ReservationDomain result =
        _orderFacade.createOrderForReservation(
            TestConstants.FAKE_RESERVATION_TOKEN, TestConstants.FAKE_TABLE_TOKEN, List.of(dishReq));

    assertNotNull(result);
    assertEquals(100, result.totalPrice());
    assertEquals(1, result.dishes().size());
    assertEquals("Pizza", result.dishes().getFirst().dishName());
    assertEquals(50, result.dishes().getFirst().price());

    verify(_orderRepo, times(1)).saveOrderWithItems(any(Orders.class), anyList());

    verify(_notification, times(1))
        .sendEventToTopic(
            eq("/orders/updates"),
            argThat(
                event ->
                    event.getEventType() == WebSocketEventType.CREATED
                        && event.getEntityType().equals("ORDER")
                        && "NEW_ORDER_TOKEN".equals(event.getToken())
                        && event.getPayload() != null
                        && TestConstants.FAKE_RESERVATION_TOKEN.equals(
                            ((SyncOrderResponse) event.getPayload()).getReservationToken())));
  }

  @Test
  @DisplayName("Create order for reservation: Should return empty domain when no dishes requested")
  void createOrderForReservation_ShouldReturnEmptyDomain_WhenNoDishesRequested() {
    ReservationDomain result =
        _orderFacade.createOrderForReservation(
            TestConstants.FAKE_RESERVATION_TOKEN, TestConstants.FAKE_TABLE_TOKEN, List.of());

    assertNotNull(result);
    assertEquals(0, result.totalPrice());
    assertTrue(result.dishes().isEmpty());

    verify(_orderRepo, never()).saveOrderWithItems(any(), any());
  }

  @Test
  @DisplayName("Create order for reservation: Should throw exception when dish not found in map")
  void createOrderForReservation_ShouldThrowException_WhenDishNotFoundInMap() {
    ReservationDishRequest dishReq = new ReservationDishRequest();
    dishReq.setDishToken(TestConstants.FAKE_DISH_TOKEN);
    dishReq.setQuantity(2);

    when(_reservationRepo.findByToken(TestConstants.FAKE_RESERVATION_TOKEN))
        .thenReturn(Optional.of(new Reservations()));
    when(_tableRepo.findByToken(TestConstants.FAKE_TABLE_TOKEN)).thenReturn(new RestaurantTables());
    when(_orderRepo.findStatusByToken("PENDING")).thenReturn(new OrderStatus());

    when(_dishRepo.listForOrder(List.of(TestConstants.FAKE_DISH_TOKEN))).thenReturn(List.of());

    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> createOrderForReservation(dishReq));

    assertTrue(exception.getMessage().contains("Dish not found"));
    verify(_orderRepo, never()).saveOrderWithItems(any(), any());
  }

  @Test
  @DisplayName("Get order summary for reservation: should return empty  when order not exist")
  void getOrderSummaryForReservation_ShouldReturnEmpty_WhenNoOrderExists() {
    when(_orderRepo.findByReservationToken(TestConstants.FAKE_RESERVATION_TOKEN))
        .thenReturn(Optional.empty());

    OrderSummaryDomain result =
        _orderFacade.getOrderSummaryForReservation(TestConstants.FAKE_RESERVATION_TOKEN);

    assertNotNull(result);
    assertEquals(0, result.totalPrice());
    assertTrue(result.dishes().isEmpty());
  }

  @Test
  @DisplayName(
      "Get order summary for reservation: should return mapped dishes with translated status when order exist")
  void getOrderSummaryForReservation_ShouldReturnMappedDishes_WhenOrderExists() {
    LocaleContextHolder.setLocale(Locale.of(TestConstants.LANG_PL));

    Orders mockOrder = new Orders();
    mockOrder.setToken(TestConstants.FAKE_ORDER_TOKEN);
    mockOrder.setTotalPrice(150);

    Dishes mockDish = new Dishes();
    mockDish.setName("Pizza");

    OrderItemsStatus mockStatus = new OrderItemsStatus();
    mockStatus.setNamePl("W Przygotowaniu");
    mockStatus.setNameEn("In Progress");

    OrderItems mockItem = new OrderItems();
    mockItem.setProduct(mockDish);
    mockItem.setQuantity(3);
    mockItem.setPriceAtTimeOfOrder(50);
    mockItem.setStatuses(Set.of(mockStatus));

    when(_orderRepo.findByReservationToken(TestConstants.FAKE_RESERVATION_TOKEN))
        .thenReturn(Optional.of(mockOrder));
    when(_orderRepo.findItemsByOrderToken(TestConstants.FAKE_ORDER_TOKEN))
        .thenReturn(List.of(mockItem));

    OrderSummaryDomain result =
        _orderFacade.getOrderSummaryForReservation(TestConstants.FAKE_RESERVATION_TOKEN);

    assertNotNull(result);
    assertEquals(150, result.totalPrice());
    assertEquals(1, result.dishes().size());
    assertEquals("Pizza", result.dishes().getFirst().getDishName());
    assertEquals(50, result.dishes().getFirst().getPrice());
    assertEquals(3, result.dishes().getFirst().getQuantity());
    assertEquals("W Przygotowaniu", result.dishes().getFirst().getStatus());
  }

  @Test
  @DisplayName("Today order details: should return empty when order not foudn")
  void todayOrderDetails_ShouldReturnEmptyDomain_WhenOrderNotFound() {
    when(_orderRepo.findByReservationToken("fake-res-token")).thenReturn(Optional.empty());

    TodayOrderSummaryDomain result = _orderFacade.todayOrderDetails("fake-res-token", "pl");

    assertEquals(0, result.totalPrice());
    assertTrue(result.dishes().isEmpty());

    verify(_orderRepo, never()).findItemsByOrderToken(anyString());
  }

  @Test
  @DisplayName("Today order details: should return list when order found")
  void todayOrderDetails_ShouldReturnMappedData() {
    Orders mockOrder = new Orders();
    mockOrder.setToken("fake-order-token");
    mockOrder.setTotalPrice(120);

    Allergens gluten = new Allergens();
    gluten.setNamePl("Gluten");

    Allergens lactose = new Allergens();
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

    when(_orderRepo.findByReservationToken("fake-res-token")).thenReturn(Optional.of(mockOrder));
    when(_orderRepo.findItemsByOrderToken("fake-order-token")).thenReturn(List.of(mockItem));

    TodayOrderSummaryDomain result = _orderFacade.todayOrderDetails("fake-res-token", "pl");

    assertEquals(120, result.totalPrice());
    assertEquals(1, result.dishes().size());

    TodayReservationDishResponse dishRes = result.dishes().getFirst();
    assertEquals("Spaghetti", dishRes.getDishName());
    assertEquals(60, dishRes.getPrice());
    assertEquals(2, dishRes.getQuantity());
    assertEquals("Bez soli", dishRes.getNote());

    assertEquals(2, dishRes.getAllergens().size());
    assertTrue(dishRes.getAllergens().contains("Gluten"));
    assertTrue(dishRes.getAllergens().contains("Laktoza"));
  }

  @Test
  @DisplayName("Today order details: should return empty when ingredients not found")
  void todayOrderDetails_ShouldReturnEmptyLists_WhenDishHasNoIngredients() {
    Orders mockOrder = new Orders();
    mockOrder.setToken("fake-order-token");

    Dishes mockDish = new Dishes();
    mockDish.setIngredients(null);

    OrderItems mockItem = new OrderItems();
    mockItem.setProduct(mockDish);
    mockItem.setQuantity(1);
    mockItem.setPriceAtTimeOfOrder(50);

    when(_orderRepo.findByReservationToken(anyString())).thenReturn(Optional.of(mockOrder));
    when(_orderRepo.findItemsByOrderToken(anyString())).thenReturn(List.of(mockItem));

    TodayOrderSummaryDomain result = _orderFacade.todayOrderDetails("res-token", "pl");

    assertTrue(result.dishes().getFirst().getIngredient().isEmpty());
    assertTrue(result.dishes().getFirst().getAllergens().isEmpty());
  }

  @Test
  @DisplayName("Remove item form reservation: should throw exception when dish in order not found")
  void removeItemFromReservation_ShouldThrowException_WhenDishNotFoundInOrder() {
    Orders mockOrder = new Orders();
    Users waiter = new Users();
    waiter.setToken("waiter-token");
    mockOrder.setWaiter(waiter);

    when(_orderRepo.findByReservationToken(anyString())).thenReturn(Optional.of(mockOrder));
    when(_orderRepo.findItemsByOrderToken(any())).thenReturn(List.of());
    ReservationDishRequest req = new ReservationDishRequest();
    req.setDishToken("MISSING_DISH");

    assertThrows(
        RuntimeException.class,
        () -> _orderFacade.removeItemFromReservation("waiter-token", "res-token", req));
  }

  @Test
  @DisplayName(
      "Remove item form reservation: should decrease quantity and create cancelled when partial remove")
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

    OrderItemsStatus pendingStatus = new OrderItemsStatus();
    pendingStatus.setToken("PENDING");

    OrderItems mockItem = new OrderItems();
    mockItem.setProduct(mockDish);
    mockItem.setQuantity(3);
    mockItem.setPriceAtTimeOfOrder(50);
    mockItem.setNote(null);
    mockItem.setStatuses(new HashSet<>(Set.of(pendingStatus)));

    OrderItemsStatus cancelledStatus = new OrderItemsStatus();
    cancelledStatus.setToken("CANCELLED");

    when(_orderRepo.findByReservationToken(TestConstants.FAKE_RESERVATION_TOKEN))
        .thenReturn(Optional.of(mockOrder));
    when(_orderRepo.findItemsByOrderToken(TestConstants.FAKE_ORDER_TOKEN))
        .thenReturn(List.of(mockItem));
    when(_orderRepo.findItemStatusByToken("CANCELLED")).thenReturn(cancelledStatus);
    when(_orderRepo.findItemStatusByToken("PENDING")).thenReturn(pendingStatus);

    _orderFacade.removeItemFromReservation(
        TestConstants.FAKE_USER_TOKEN, TestConstants.FAKE_RESERVATION_TOKEN, request);

    assertEquals(2, mockItem.getQuantity());
    assertEquals(100, mockOrder.getTotalPrice());

    verify(_orderRepo, times(2)).saveItem(any(OrderItems.class));
    verify(_orderRepo, times(1)).save(mockOrder);

    verify(_notification, times(1))
        .sendEventToTopic(
            eq("/orders/updates"),
            argThat(
                event ->
                    event.getEventType() == WebSocketEventType.UPDATED // Zmieniono z DELETED
                        && event.getEntityType().equals("ORDER")
                        && ((SyncOrderResponse) event.getPayload()).getTotalPrice() == 100));

    verify(_notification, times(1)).sendEventToTopic(eq("/orders/items"), any());
  }

  @Test
  @DisplayName(
      "Remove item form reservation: should set status to cancelled when every dish is remove")
  void removeItemFromReservation_ShouldSetStatusToCancelled_WhenFullRemoval() {
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

    OrderItemsStatus pendingStatus = new OrderItemsStatus();
    pendingStatus.setToken("PENDING");

    OrderItems mockItem = new OrderItems();
    mockItem.setProduct(mockDish);
    mockItem.setQuantity(2);
    mockItem.setPriceAtTimeOfOrder(50);
    mockItem.setNote(note);
    mockItem.setStatuses(new HashSet<>(Set.of(pendingStatus)));

    OrderItemsStatus cancelledStatus = new OrderItemsStatus();
    cancelledStatus.setToken("CANCELLED");

    when(_orderRepo.findByReservationToken(TestConstants.FAKE_RESERVATION_TOKEN))
        .thenReturn(Optional.of(mockOrder));
    when(_orderRepo.findItemsByOrderToken(TestConstants.FAKE_ORDER_TOKEN))
        .thenReturn(List.of(mockItem));
    when(_orderRepo.findItemStatusByToken("CANCELLED")).thenReturn(cancelledStatus);

    _orderFacade.removeItemFromReservation(
        TestConstants.FAKE_USER_TOKEN, TestConstants.FAKE_RESERVATION_TOKEN, request);

    assertEquals(0, mockOrder.getTotalPrice());
    assertTrue(mockItem.getStatuses().contains(cancelledStatus));

    verify(_orderRepo, times(1)).saveItem(mockItem);
    verify(_orderRepo, times(1)).save(mockOrder);

    verify(_notification, times(1))
        .sendEventToTopic(
            eq("/orders/updates"),
            argThat(
                event ->
                    event.getEventType() == WebSocketEventType.UPDATED
                        && event.getEntityType().equals("ORDER")
                        && event.getToken().equals(TestConstants.FAKE_ORDER_TOKEN)
                        && ((SyncOrderResponse) event.getPayload()).getTotalPrice() == 0));

    verify(_notification, times(1))
        .sendEventToTopic(
            eq("/orders/items"),
            argThat(
                event ->
                    event.getEntityType().equals("ORDER_ITEM")
                        && ((SyncOrderItemResponse) event.getPayload())
                            .getStatusTokens()
                            .contains("CANCELLED")));
  }

  @Test
  @DisplayName(
      "Add item from reservation: should increase quantiti and add new item with corrcet price")
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

    OrderItemsStatus pendingItemStatus = new OrderItemsStatus();
    pendingItemStatus.setToken("PENDING");

    when(_orderRepo.findByReservationToken(TestConstants.FAKE_RESERVATION_TOKEN))
        .thenReturn(Optional.of(mockOrder));
    when(_orderRepo.findItemsByOrderToken(TestConstants.FAKE_ORDER_TOKEN))
        .thenReturn(List.of(existingItem));
    when(_dishRepo.listForOrder(anyList())).thenReturn(List.of(existingDish, newDish));
    when(_orderRepo.findItemStatusByToken("PENDING")).thenReturn(pendingItemStatus);

    _orderFacade.addItemFromReservation(
        TestConstants.FAKE_USER_TOKEN, TestConstants.FAKE_RESERVATION_TOKEN, requests);

    assertEquals(3, existingItem.getQuantity());
    assertEquals(210, mockOrder.getTotalPrice());

    verify(_orderRepo, times(2)).saveItem(any(OrderItems.class));
    verify(_orderRepo, times(1)).save(mockOrder);

    verify(_notification, times(1))
        .sendEventToTopic(
            eq("/orders/updates"),
            argThat(
                event ->
                    event.getEventType() == WebSocketEventType.UPDATED
                        && event.getEntityType().equals("ORDER")
                        && event.getToken().equals(TestConstants.FAKE_ORDER_TOKEN)
                        && event.getPayload() != null));
  }

  @Test
  @DisplayName("Add item from reservation: should throw exception when waiter is not assigned")
  void addItemFromReservation_ShouldThrowException_WhenWaiterIsNotAssignedToOrder() {
    String assignedWaiterToken = "waiterA";
    String intruderWaiterToken = "waiterB";

    Users mockWaiter = new Users();
    mockWaiter.setToken(assignedWaiterToken);

    Orders mockOrder = new Orders();
    mockOrder.setWaiter(mockWaiter);

    when(_orderRepo.findByReservationToken(TestConstants.FAKE_RESERVATION_TOKEN))
        .thenReturn(Optional.of(mockOrder));

    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> addItemFromReservation(intruderWaiterToken));

    assertEquals("Order not found or you are not the assigned waiter", exception.getMessage());
    verify(_orderRepo, never()).save(any());
  }

  @Test
  @DisplayName("Assign waiter to order: should assign waiter and only change pending time")
  void assignWaiterToOrders_ShouldAssignWaiterAndOnlyChangePendingItems() {
    String inProgressStatus = "IN_PROGRESS";

    Orders mockOrder = new Orders();
    mockOrder.setToken(TestConstants.FAKE_ORDER_TOKEN);

    OrderStatus pendingOrderStatus = new OrderStatus();
    pendingOrderStatus.setToken("PENDING");
    mockOrder.setStatuses(new HashSet<>(Set.of(pendingOrderStatus)));

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

    when(_orderRepo.findByReservationToken(TestConstants.FAKE_RESERVATION_TOKEN))
        .thenReturn(Optional.of(mockOrder));
    when(_userRepo.findByToken(TestConstants.FAKE_USER_TOKEN)).thenReturn(mockWaiter);

    when(_orderRepo.findStatusByToken(inProgressStatus)).thenReturn(inProgressOrder);
    when(_orderRepo.findItemStatusByToken(inProgressStatus)).thenReturn(inProgressItem);

    when(_orderRepo.findItemsByOrderToken(mockOrder.getToken()))
        .thenReturn(List.of(pendingDish, emptyStatusDish, cancelledDish));

    _orderFacade.assignWaiterToOrders(
        TestConstants.FAKE_RESERVATION_TOKEN, TestConstants.FAKE_USER_TOKEN);

    assertEquals(mockWaiter, mockOrder.getWaiter());
    assertTrue(mockOrder.getStatuses().contains(inProgressOrder));

    assertTrue(pendingDish.getStatuses().contains(inProgressItem));
    assertTrue(emptyStatusDish.getStatuses().contains(inProgressItem));

    assertFalse(cancelledDish.getStatuses().contains(inProgressItem));
    assertTrue(cancelledDish.getStatuses().contains(cancelledItemStatus));

    verify(_orderRepo, times(1)).saveAllItems(anyList());
    verify(_orderRepo, times(1)).save(mockOrder);

    verify(_notification, never()).sendEventToTopic(anyString(), any());
  }

  @Test
  @DisplayName("is Absent: should do nothing when order dosen't exist")
  void isAbsent_ShouldDoNothing_WhenOrderDoesNotExist() {
    when(_orderRepo.findByReservationToken(TestConstants.FAKE_RESERVATION_TOKEN))
        .thenReturn(Optional.empty());

    _orderFacade.isAbsent(TestConstants.FAKE_RESERVATION_TOKEN);

    verify(_orderRepo, never()).findStatusByToken(anyString());
    verify(_orderRepo, never()).save(any());
  }

  @Test
  @DisplayName("is Absent: should cancel order and items when order exists")
  void isAbsent_ShouldCancelOrderAndItems_WhenOrderExists() {
    Orders mockOrder = new Orders();
    mockOrder.setToken(TestConstants.FAKE_ORDER_TOKEN);

    OrderStatus pendingOrderStatus = new OrderStatus();
    pendingOrderStatus.setToken("PENDING");
    mockOrder.setStatuses(new HashSet<>(Set.of(pendingOrderStatus)));

    OrderItemsStatus pendingItemStatus = new OrderItemsStatus();
    pendingItemStatus.setToken("PENDING");

    OrderItems mockItem1 = new OrderItems();
    mockItem1.setStatuses(new HashSet<>(Set.of(pendingItemStatus)));
    OrderItems mockItem2 = new OrderItems();
    mockItem2.setStatuses(new HashSet<>(Set.of(pendingItemStatus)));
    List<OrderItems> orderItems = List.of(mockItem1, mockItem2);

    OrderStatus cancelledStatus = new OrderStatus();
    cancelledStatus.setToken("CANCELLED");

    OrderItemsStatus cancelledItemStatus = new OrderItemsStatus();
    cancelledItemStatus.setToken("CANCELLED");

    when(_orderRepo.findByReservationToken(TestConstants.FAKE_RESERVATION_TOKEN))
        .thenReturn(Optional.of(mockOrder));

    when(_orderRepo.findStatusByToken("CANCELLED")).thenReturn(cancelledStatus);
    when(_orderRepo.findItemStatusByToken("CANCELLED")).thenReturn(cancelledItemStatus);
    when(_orderRepo.findItemsByOrderToken(mockOrder.getToken())).thenReturn(orderItems);

    _orderFacade.isAbsent(TestConstants.FAKE_RESERVATION_TOKEN);

    assertTrue(mockOrder.getStatuses().contains(cancelledStatus));
    assertTrue(mockItem1.getStatuses().contains(cancelledItemStatus));
    assertTrue(mockItem2.getStatuses().contains(cancelledItemStatus));

    verify(_orderRepo, times(1)).saveAllItems(orderItems);
    verify(_orderRepo, times(1)).save(mockOrder);

    verify(_notification, times(1))
        .sendEventToTopic(
            eq("/orders/updates"),
            argThat(
                event ->
                    event.getEventType() == WebSocketEventType.UPDATED
                        && event.getEntityType().equals("ORDER")
                        && event.getToken().equals(TestConstants.FAKE_ORDER_TOKEN)
                        && event.getPayload() != null));
  }

  @Test
  @DisplayName("getDictionary: Returns empty list when repository returns empty")
  void getDictionary_ShouldReturnEmptyList_WhenRepoReturnsEmpty() {
    when(_orderRepo.findAllStatuses()).thenReturn(new java.util.ArrayList<>());
    DictionaryResponse result = _orderFacade.getDictionary();
    assertTrue(result.getItem().isEmpty());
  }

  @Test
  @DisplayName("getDictionary: Returns Polish names when language is pl")
  void getDictionary_ShouldReturnPolishNames_WhenLanguageIsPl() {
    LocaleContextHolder.setLocale(Locale.of(TestConstants.LANG_PL));

    OrderStatus status = new OrderStatus();
    status.setToken("PENDING");
    status.setNamePl("Oczekujące PL");
    status.setNameEn("Pending EN");

    when(_orderRepo.findAllStatuses()).thenReturn(List.of(status));

    DictionaryResponse result = _orderFacade.getDictionary();

    assertEquals(1, result.getItem().size());
    assertEquals("PENDING", result.getItem().getFirst().getToken());
    assertEquals("Oczekujące PL", result.getItem().getFirst().getName());
  }

  @Test
  @DisplayName("getDictionary: Returns English names when language is not pl")
  void getDictionary_ShouldReturnEnglishNames_WhenLanguageIsNotPl() {
    Locale.of(TestConstants.LANG_EN);
    OrderStatus status = new OrderStatus();
    status.setToken("COMPLETED");
    status.setNamePl("Zakończone PL");
    status.setNameEn("Completed EN");

    when(_orderRepo.findAllStatuses()).thenReturn(List.of(status));

    DictionaryResponse result = _orderFacade.getDictionary();

    assertEquals(1, result.getItem().size());
    assertEquals("COMPLETED", result.getItem().getFirst().getToken());
    assertEquals("Completed EN", result.getItem().getFirst().getName());
  }

  @Test
  @DisplayName("getItemStatusesDictionary: Returns empty list when repository returns empty")
  void getItemStatusesDictionary_ShouldReturnEmptyList_WhenRepoReturnsEmpty() {
    when(_orderRepo.findAllItemStatuses()).thenReturn(new java.util.ArrayList<>());
    DictionaryResponse result = _orderFacade.getItemStatusesDictionary();
    assertTrue(result.getItem().isEmpty());
  }

  @Test
  @DisplayName("getItemStatusesDictionary: Returns Polish names when language is pl")
  void getItemStatusesDictionary_ShouldReturnPolishNames_WhenLanguageIsPl() {
    LocaleContextHolder.setLocale(Locale.of(TestConstants.LANG_PL));
    OrderItemsStatus status = new OrderItemsStatus();
    status.setToken(TestConstants.STATUS_READY);
    status.setNamePl("Gotowe PL");
    status.setNameEn("Ready EN");

    when(_orderRepo.findAllItemStatuses()).thenReturn(List.of(status));

    DictionaryResponse result = _orderFacade.getItemStatusesDictionary();

    assertEquals(1, result.getItem().size());
    assertEquals(TestConstants.STATUS_READY, result.getItem().getFirst().getToken());
    assertEquals("Gotowe PL", result.getItem().getFirst().getName());
  }

  @Test
  @DisplayName("getItemStatusesDictionary: Returns English names when language is not pl")
  void getItemStatusesDictionary_ShouldReturnEnglishNames_WhenLanguageIsNotPl() {
    Locale.of(TestConstants.LANG_EN);
    OrderItemsStatus status = new OrderItemsStatus();
    status.setToken(TestConstants.STATUS_READY);
    status.setNamePl("Gotowe PL");
    status.setNameEn("Ready EN");

    when(_orderRepo.findAllItemStatuses()).thenReturn(List.of(status));

    DictionaryResponse result = _orderFacade.getItemStatusesDictionary();

    assertEquals(1, result.getItem().size());
    assertEquals(TestConstants.STATUS_READY, result.getItem().getFirst().getToken());
    assertEquals("Ready EN", result.getItem().getFirst().getName());
  }

  @Test
  @DisplayName("addStatus: Should save order status when data is correct")
  void addStatus_ShouldSaveOrderStatus_WhenDataIsCorrect() {
    AddEntityRequest request = new AddEntityRequest();
    request.setNamePl("Nowy Status PL");
    request.setNameEn("New Status EN");

    when(_orderRepo.isStatusNameTaken(anyString(), anyString())).thenReturn(false);

    doAnswer(
            invocation -> {
              OrderStatus status = invocation.getArgument(0);
              status.setToken("NEW_STATUS_EN");
              return null;
            })
        .when(_orderRepo)
        .saveStatus(any(OrderStatus.class));

    assertDoesNotThrow(() -> _orderFacade.addStatus(request));

    verify(_orderRepo, times(1))
        .saveStatus(
            argThat(
                status ->
                    status.getNamePl().equals("Nowy Status PL")
                        && status.getNameEn().equals("New Status EN")
                        && status.getToken().equals("NEW_STATUS_EN")));

    verify(_notification, times(1))
        .sendEventToTopic(
            eq("/dictionary/order-statuses"),
            argThat(
                event ->
                    event.getEventType() == WebSocketEventType.CREATED
                        && event.getEntityType().equals("ORDER_STATUS")
                        && event.getPayload() != null
                        && "Nowy Status PL"
                            .equals(((SyncDictionaryResponse) event.getPayload()).getNamePl())));
  }

  @Test
  @DisplayName("addItemStatus: Should save order item status when data is correct")
  void addItemStatus_ShouldSaveOrderItemStatus_WhenDataIsCorrect() {
    AddEntityRequest request = new AddEntityRequest();
    request.setNamePl("Nowy Status Elementu PL");
    request.setNameEn("New Item Status EN");

    when(_orderRepo.isItemStatusNameTaken(anyString(), anyString())).thenReturn(false);

    doAnswer(
            invocation -> {
              OrderItemsStatus status = invocation.getArgument(0);
              status.setToken("NEW_ITEM_STATUS_EN");
              return null;
            })
        .when(_orderRepo)
        .saveItemStatus(any(OrderItemsStatus.class));

    assertDoesNotThrow(() -> _orderFacade.addItemStatus(request));

    verify(_orderRepo, times(1))
        .saveItemStatus(
            argThat(
                status ->
                    status.getNamePl().equals("Nowy Status Elementu PL")
                        && status.getNameEn().equals("New Item Status EN")
                        && status.getToken().equals("NEW_ITEM_STATUS_EN")));

    verify(_notification, times(1))
        .sendEventToTopic(
            eq("/dictionary/order-item-statuses"),
            argThat(
                event ->
                    event.getEventType() == WebSocketEventType.CREATED
                        && event.getEntityType().equals("ORDER_ITEM_STATUS")
                        && event.getPayload() != null
                        && "Nowy Status Elementu PL"
                            .equals(((SyncDictionaryResponse) event.getPayload()).getNamePl())));
  }

  @Test
  @DisplayName(
      "removeStatus: Should soft delete order status and reassign associated orders to OTHER")
  void removeStatus_ShouldSoftDelete_AndReassignOrdersToOther() {
    String tokenToRemove = "PENDING_TOKEN";

    OrderStatus statusToRemove = new OrderStatus();
    statusToRemove.setId(java.util.UUID.randomUUID());
    statusToRemove.setToken(tokenToRemove);
    statusToRemove.setNameEn("Pending");
    statusToRemove.setNamePl("Oczekujący");

    OrderStatus fallbackStatus = new OrderStatus();
    fallbackStatus.setToken("OTHER");
    fallbackStatus.setNameEn("Other");
    fallbackStatus.setNamePl("Inne");

    Orders order1 = new Orders();
    order1.getStatuses().add(statusToRemove);

    Orders order2 = new Orders();
    order2.getStatuses().add(statusToRemove);

    List<Orders> affectedOrders = List.of(order1, order2);

    when(_orderRepo.findStatusByToken(tokenToRemove)).thenReturn(statusToRemove);
    when(_orderRepo.findStatusByToken("OTHER")).thenReturn(fallbackStatus);
    when(_orderRepo.findOrdersByStatus(statusToRemove)).thenReturn(affectedOrders);

    assertDoesNotThrow(() -> _orderFacade.removeStatus(tokenToRemove));

    assertFalse(order1.getStatuses().contains(statusToRemove));
    assertTrue(order1.getStatuses().contains(fallbackStatus));
    assertFalse(order2.getStatuses().contains(statusToRemove));
    assertTrue(order2.getStatuses().contains(fallbackStatus));
    verify(_orderRepo, times(1)).save(order1);
    verify(_orderRepo, times(1)).save(order2);

    assertTrue(statusToRemove.getToken().startsWith("DELETED_"));
    assertTrue(statusToRemove.getNameEn().startsWith("DELETED_"));
    assertNotNull(statusToRemove.getDeletedAt());
    verify(_orderRepo, times(1)).saveStatus(statusToRemove);

    verify(_notification, times(1))
        .sendEventToTopic(
            eq("/dictionary/order-statuses"),
            argThat(
                event ->
                    event.getEventType() == WebSocketEventType.DELETED
                        && event.getEntityType().equals("ORDER_STATUS")
                        && event.getToken().equals(tokenToRemove)
                        && event.getPayload() == null));
  }

  @Test
  @DisplayName(
      "removeItemStatus: Should soft delete order item status and reassign associated items to OTHER")
  void removeItemStatus_ShouldSoftDelete_AndReassignItemsToOther() {
    String tokenToRemove = "READY_TOKEN";

    OrderItemsStatus statusToRemove = new OrderItemsStatus();
    statusToRemove.setId(java.util.UUID.randomUUID());
    statusToRemove.setToken(tokenToRemove);
    statusToRemove.setNameEn("Ready");
    statusToRemove.setNamePl("Gotowe");

    OrderItemsStatus fallbackStatus = new OrderItemsStatus();
    fallbackStatus.setToken("OTHER");
    fallbackStatus.setNameEn("Other");
    fallbackStatus.setNamePl("Inne");

    OrderItems item1 = new OrderItems();
    item1.getStatuses().add(statusToRemove);

    OrderItems item2 = new OrderItems();
    item2.getStatuses().add(statusToRemove);

    List<OrderItems> affectedItems = List.of(item1, item2);

    when(_orderRepo.findItemStatusByToken(tokenToRemove)).thenReturn(statusToRemove);
    when(_orderRepo.findItemStatusByToken("OTHER")).thenReturn(fallbackStatus);
    when(_orderRepo.findOrderItemsByStatus(statusToRemove)).thenReturn(affectedItems);

    assertDoesNotThrow(() -> _orderFacade.removeItemStatus(tokenToRemove));

    assertFalse(item1.getStatuses().contains(statusToRemove));
    assertTrue(item1.getStatuses().contains(fallbackStatus));
    assertFalse(item2.getStatuses().contains(statusToRemove));
    assertTrue(item2.getStatuses().contains(fallbackStatus));
    verify(_orderRepo, times(1)).saveItem(item1);
    verify(_orderRepo, times(1)).saveItem(item2);

    assertTrue(statusToRemove.getToken().startsWith("DELETED_"));
    assertTrue(statusToRemove.getNameEn().startsWith("DELETED_"));
    assertNotNull(statusToRemove.getDeletedAt());
    verify(_orderRepo, times(1)).saveItemStatus(statusToRemove);

    verify(_notification, times(1))
        .sendEventToTopic(
            eq("/dictionary/order-item-statuses"),
            argThat(
                event ->
                    event.getEventType() == WebSocketEventType.DELETED
                        && event.getEntityType().equals("ORDER_ITEM_STATUS")
                        && event.getToken().equals(tokenToRemove)
                        && event.getPayload() == null));
  }

  private void createOrderForReservation(ReservationDishRequest dishReq) {
    _orderFacade.createOrderForReservation(
        TestConstants.FAKE_RESERVATION_TOKEN, TestConstants.FAKE_TABLE_TOKEN, List.of(dishReq));
  }

  private void addItemFromReservation(String intruderWaiterToken) {
    _orderFacade.addItemFromReservation(
        intruderWaiterToken, TestConstants.FAKE_RESERVATION_TOKEN, List.of());
  }
}
