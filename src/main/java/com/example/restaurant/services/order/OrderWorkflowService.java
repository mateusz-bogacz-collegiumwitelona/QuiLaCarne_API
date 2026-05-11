package com.example.restaurant.services.order;

import com.example.restaurant.dto.domain.ReservationDishDoamin;
import com.example.restaurant.dto.domain.ReservationDomain;
import com.example.restaurant.dto.request.ReservationDishRequest;
import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.models.OrderItems;
import com.example.restaurant.models.Orders;
import com.example.restaurant.repository.interfaces.*;
import jakarta.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderWorkflowService {
  private final IOrderRepository _orderRepo;
  private final OrderSyncPublisher _syncPublisher;
  private final IDishRepository _dishRepo;
  private final IReservationRepository _reservationRepo;
  private final ITableRespository _tableRepo;
  private final IUserRepository _userRepo;

  @Transactional
  public ReservationDomain createOrderForReservation(
      String reservationToken, String tableToken, List<ReservationDishRequest> dishesRequest) {
    if (dishesRequest.isEmpty()) return new ReservationDomain(new ArrayList<>(), 0);

    List<String> dishTokens =
        dishesRequest.stream().map(ReservationDishRequest::getDishToken).toList();

    Map<String, Dishes> dishesMap =
        _dishRepo.listForOrder(dishTokens).stream()
            .collect(Collectors.toMap(Dishes::getToken, dish -> dish));

    var reservation =
        _reservationRepo
            .findByToken(reservationToken)
            .orElseThrow(() -> new EntityNotFoundException("Reservation not found"));

    var table = _tableRepo.findByToken(tableToken);

    var status = _orderRepo.findStatusByToken("PENDING");

    Orders order = new Orders();
    order.setReservation(reservation);
    order.setTable(table);
    order.setStatuses(Set.of(status));

    int totalPrice = 0;
    List<OrderItems> orderItems = new ArrayList<>();
    List<ReservationDishDoamin> reservationDishes = new ArrayList<>();

    for (ReservationDishRequest req : dishesRequest) {
      var dish = dishesMap.get(req.getDishToken());

      if (dish == null) throw new EntityNotFoundException("Dish not found: " + req.getDishToken());

      int itemTotalPrice = dish.getPrice() * req.getQuantity();
      totalPrice += itemTotalPrice;

      OrderItems item = new OrderItems();
      item.setOrder(order);
      item.setProduct(dish);
      item.setQuantity(req.getQuantity());
      item.setPriceAtTimeOfOrder(dish.getPrice());
      item.setNote(req.getNote());
      item.setStatuses(Set.of());

      orderItems.add(item);

      reservationDishes.add(
          new ReservationDishDoamin(dish.getName(), dish.getPrice(), req.getQuantity()));
    }

    order.setTotalPrice(totalPrice);

    _orderRepo.saveOrderWithItems(order, orderItems);

    _syncPublisher.publishOrderCreated(order, orderItems);

    return new ReservationDomain(reservationDishes, totalPrice);
  }

  @Transactional
  public void addItemFromReservation(
      String waiterToken, String reservationToken, List<ReservationDishRequest> request) {
    Orders order = getOrderAndValidateWaiter(reservationToken, waiterToken);

    List<OrderItems> updatedItems = processRequestedItems(order, request);

    _orderRepo.save(order);

    _syncPublisher.publishOrderUpdated(order, updatedItems);
  }

  @Transactional
  public void assignWaiterToOrders(String reservationToken, String waiterToken) {
    var order =
        _orderRepo
            .findByReservationToken(reservationToken)
            .orElseThrow(() -> new EntityNotFoundException("Reservation not found"));

    var waiter = _userRepo.findByToken(waiterToken);

    var orderStatus = _orderRepo.findStatusByToken("IN_PROGRESS");
    var orderItemsStatus = _orderRepo.findItemStatusByToken("IN_PROGRESS");

    order.setStatuses(new HashSet<>(Set.of(orderStatus)));
    order.setWaiter(waiter);

    List<OrderItems> items = _orderRepo.findItemsByOrderToken(order.getToken());

    for (OrderItems item : items) {
      boolean isPending = item.getStatuses().stream().anyMatch(s -> "PENDING".equals(s.getToken()));

      if (isPending || item.getStatuses().isEmpty())
        item.setStatuses(new HashSet<>(Set.of(orderItemsStatus)));
    }

    _orderRepo.saveAllItems(items);
    _orderRepo.save(order);
  }

  @Transactional
  public void isAbsent(String reservationToken) {
    var orderOpt = _orderRepo.findByReservationToken(reservationToken);

    if (orderOpt.isPresent()) {
      var order = orderOpt.get();
      var orderStatus = _orderRepo.findStatusByToken("CANCELLED");
      var orderItemsStatus = _orderRepo.findItemStatusByToken("CANCELLED");

      order.setStatuses(new HashSet<>(Set.of(orderStatus)));

      List<OrderItems> items = _orderRepo.findItemsByOrderToken(order.getToken());
      for (OrderItems item : items) {
        item.setStatuses(new HashSet<>(Set.of(orderItemsStatus)));
      }

      _orderRepo.saveAllItems(items);
      _orderRepo.save(order);

      _syncPublisher.publishOrderUpdated(order, items);
    }
  }

  private List<OrderItems> processRequestedItems(
      Orders order, List<ReservationDishRequest> request) {
    List<OrderItems> existingItems = _orderRepo.findItemsByOrderToken(order.getToken());
    List<String> requestedDishTokens =
        request.stream().map(ReservationDishRequest::getDishToken).toList();
    List<Dishes> allRequestedDishes = _dishRepo.listForOrder(requestedDishTokens);

    int addToPrice = 0;
    List<OrderItems> updatedItems = new ArrayList<>();

    for (ReservationDishRequest r : request) {
      Dishes dish =
          allRequestedDishes.stream()
              .filter(d -> r.getDishToken().equals(d.getToken()))
              .findFirst()
              .orElseThrow(() -> new RuntimeException("Dish not found: " + r.getDishToken()));

      String reqNote = normalizeNote(r.getNote());

      Optional<OrderItems> existingItemOpt =
          existingItems.stream()
              .filter(
                  i ->
                      dish.getToken().equals(i.getProduct().getToken())
                          && Objects.equals(reqNote, normalizeNote(i.getNote())))
              .filter(
                  i -> i.getStatuses().stream().noneMatch(s -> "CANCELLED".equals(s.getToken())))
              .findFirst();

      OrderItems item;
      if (existingItemOpt.isPresent()) {
        item = existingItemOpt.get();
        item.setQuantity(item.getQuantity() + r.getQuantity());
        _orderRepo.saveItem(item);

        addToPrice += item.getPriceAtTimeOfOrder() * r.getQuantity();
      } else {
        item = new OrderItems();
        item.setOrder(order);
        item.setProduct(dish);
        item.setQuantity(r.getQuantity());
        item.setPriceAtTimeOfOrder(dish.getPrice());
        item.setNote(reqNote);
        _orderRepo.saveItem(item);

        addToPrice += dish.getPrice() * r.getQuantity();
      }
      updatedItems.add(item);
    }
    order.setTotalPrice(order.getTotalPrice() + addToPrice);

    return updatedItems;
  }

  @Transactional
  public void removeItemFromReservation(
      String waiterToken, String reservationToken, ReservationDishRequest request) {
    Orders order =
        _orderRepo
            .findByReservationToken(reservationToken)
            .filter(o -> o.getWaiter() != null && waiterToken.equals(o.getWaiter().getToken()))
            .orElseThrow(() -> new EntityNotFoundException("Assigned order not found"));

    List<OrderItems> items = _orderRepo.findItemsByOrderToken(order.getToken());

    String reqNote = normalizeNote(request.getNote());

    OrderItems itemToMod =
        items.stream()
            .filter(
                i ->
                    request.getDishToken().equals(i.getProduct().getToken())
                        && Objects.equals(reqNote, normalizeNote(i.getNote())))
            .filter(i -> i.getStatuses().stream().noneMatch(s -> "CANCELLED".equals(s.getToken())))
            .findFirst()
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Active dish with specified note not found in the order"));

    int currentQuantity = itemToMod.getQuantity();
    int quantityToRemove = request.getQuantity();
    int pricePerItem = itemToMod.getPriceAtTimeOfOrder();

    var cancelledStatus = _orderRepo.findItemStatusByToken("CANCELLED");

    if (quantityToRemove >= currentQuantity) {
      order.setTotalPrice(order.getTotalPrice() - (pricePerItem * currentQuantity));

      itemToMod.setStatuses(new HashSet<>(Set.of(cancelledStatus)));
      _orderRepo.saveItem(itemToMod);
    } else {
      itemToMod.setQuantity(currentQuantity - quantityToRemove);
      order.setTotalPrice(order.getTotalPrice() - (pricePerItem * quantityToRemove));
      _orderRepo.saveItem(itemToMod);

      OrderItems cancelledItem = new OrderItems();
      cancelledItem.setOrder(order);
      cancelledItem.setProduct(itemToMod.getProduct());
      cancelledItem.setQuantity(quantityToRemove);
      cancelledItem.setPriceAtTimeOfOrder(pricePerItem);
      cancelledItem.setNote(itemToMod.getNote());
      cancelledItem.setStatuses(new HashSet<>(Set.of(cancelledStatus)));

      _orderRepo.saveItem(cancelledItem);
    }
    _orderRepo.save(order);

    _syncPublisher.publishOrderUpdated(order, items);
  }

  private Orders getOrderAndValidateWaiter(String reservationToken, String waiterToken) {
    return _orderRepo
        .findByReservationToken(reservationToken)
        .filter(o -> o.getWaiter() != null && waiterToken.equals(o.getWaiter().getToken()))
        .orElseThrow(
            () -> new RuntimeException("Order not found or you are not the assigned waiter"));
  }

  private String normalizeNote(String note) {
    if (note == null || note.trim().isEmpty()) return note;
    return note.trim();
  }
}
