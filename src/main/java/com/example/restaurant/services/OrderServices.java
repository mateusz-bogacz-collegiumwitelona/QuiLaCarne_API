package com.example.restaurant.services;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.domain.OrderSummaryDomain;
import com.example.restaurant.dto.domain.ReservationDishDoamin;
import com.example.restaurant.dto.domain.ReservationDomain;
import com.example.restaurant.dto.domain.TodayOrderSummaryDomain;
import com.example.restaurant.dto.request.AddEntityRequest;
import com.example.restaurant.dto.request.ReservationDishRequest;
import com.example.restaurant.dto.response.DictionaryResponse;
import com.example.restaurant.dto.response.ReservationDishResponse;
import com.example.restaurant.dto.response.TodayReservationDishResponse;
import com.example.restaurant.dto.sync.SyncDictionaryResponse;
import com.example.restaurant.dto.sync.SyncOrderItemResponse;
import com.example.restaurant.dto.sync.SyncOrderResponse;
import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.helpers.DictionaryHelper;
import com.example.restaurant.helpers.WebSocketEvent;
import com.example.restaurant.mappers.SyncMapper;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.models.OrderItems;
import com.example.restaurant.models.Orders;
import com.example.restaurant.models.lookup.OrderItemsStatus;
import com.example.restaurant.models.lookup.OrderStatus;
import com.example.restaurant.repository.interfaces.*;
import com.example.restaurant.services.interfaces.IOrderServices;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings({"PMD.TooManyMethods", "PMD.CouplingBetweenObjects", "PMD.GodClass"})
public class OrderServices implements IOrderServices {
    private final IOrderRepository _orderRepo;
    private final IDishRepository _dishRepo;
    private final IReservationRepository _reservationRepo;
    private final ITableRespository _tableRepo;
    private final IUserRepository _userRepo;
    private final NotificationServices _notification;

    private final SyncMapper _syncMapper;

    private static final String ORDER_ENTITY_TYPE = "ORDER";
    private static final String ORDER_STATUS_ENTITY_TYPE = "ORDER_STATUS";
    private static final String ORDER_ITEM_STATUS_ENTITY_TYPE = "ORDER_ITEM_STATUS";

    @Override
    @Transactional
    public ReservationDomain createOrderForReservation(
            String reservationToken,
            String tableToken,
            List<ReservationDishRequest> dishesRequest
    ) {
        if (dishesRequest.isEmpty())
            return new ReservationDomain(new ArrayList<>(), 0);

        List<String> dishTokens = dishesRequest.stream()
                .map(ReservationDishRequest::getDishToken)
                .toList();

        Map<String, Dishes> dishesMap = _dishRepo.listForOrder(dishTokens).stream()
                .collect(Collectors.toMap(Dishes::getToken, dish -> dish));

        var reservation = _reservationRepo.findByToken(reservationToken)
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

            if (dish == null)
                throw new EntityNotFoundException("Dish not found: " + req.getDishToken());

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

            reservationDishes.add(new ReservationDishDoamin(
                    dish.getName(),
                    dish.getPrice(),
                    req.getQuantity()
            ));
        }

        order.setTotalPrice(totalPrice);

        _orderRepo.saveOrderWithItems(order, orderItems);

        sendOrderSyncEvents(order, orderItems, true);

        return new ReservationDomain(reservationDishes, totalPrice);
    }


    @Override
    public OrderSummaryDomain getOrderSummaryForReservation(String reservationToken) {
        var orderOpt = _orderRepo.findByReservationToken(reservationToken);

        if (orderOpt.isEmpty())
            return new OrderSummaryDomain(0, new ArrayList<>());

        Orders order = orderOpt.get();
        List<OrderItems> items = _orderRepo.findItemsByOrderToken(order.getToken());

        List<ReservationDishResponse> dishResponses = items.stream().map(item -> {
            ReservationDishResponse response = new ReservationDishResponse();
            response.setDishName(item.getProduct().getName());
            response.setQuantity(item.getQuantity());
            response.setPrice(item.getPriceAtTimeOfOrder());
            return response;
        }).toList();

        int totalPrice = items.stream().mapToInt(item -> item.getPriceAtTimeOfOrder() * item.getQuantity()).sum();

        return new OrderSummaryDomain(totalPrice, dishResponses);
    }

    @Override
    public TodayOrderSummaryDomain todayOrderDetails(String reservationToken, String lang) {
        var orderOpt = _orderRepo.findByReservationToken(reservationToken);

        if (orderOpt.isEmpty())
            return new TodayOrderSummaryDomain(0, new ArrayList<>());


        Orders order = orderOpt.get();
        List<OrderItems> items = _orderRepo.findItemsByOrderToken(order.getToken());

        List<TodayReservationDishResponse> dishResponses = items.stream().map(item -> {
            TodayReservationDishResponse response = new TodayReservationDishResponse();
            response.setDishName(item.getProduct().getName());
            response.setQuantity(item.getQuantity());
            response.setPrice(item.getPriceAtTimeOfOrder());
            response.setNote(item.getNote() != null ? item.getNote() : "");

            if (item.getProduct().getIngredients() != null) {
                List<String> ingredients = item.getProduct().getIngredients().stream()
                        .map(i -> i.translate(lang))
                        .toList();

                List<String> allergens = item.getProduct().getIngredients().stream()
                        .flatMap(i -> i.getAllergens().stream())
                        .map(a -> a.translate(lang))
                        .distinct()
                        .toList();

                response.setIngredient(ingredients);
                response.setAllergens(allergens);
            } else {
                response.setIngredient(new ArrayList<>());
                response.setAllergens(new ArrayList<>());
            }

            return response;
        }).toList();

        int totalPrice = items.stream().mapToInt(item -> item.getPriceAtTimeOfOrder() * item.getQuantity()).sum();

        return new TodayOrderSummaryDomain(totalPrice, dishResponses);
    }

    @Override
    @Transactional
    public void removeItemFromReservation(String waiterToken, String reservationToken, ReservationDishRequest request) {
        Orders order = _orderRepo.findByReservationToken(reservationToken)
                .filter(o -> o.getWaiter() != null && waiterToken.equals(o.getWaiter().getToken()))
                .orElseThrow(() -> new EntityNotFoundException("Assigned order not found"));

        List<OrderItems> items = _orderRepo.findItemsByOrderToken(order.getToken());

        String reqNote = normalizeNote(request.getNote());

        OrderItems itemToMod = items.stream()
                .filter(i -> request.getDishToken().equals(i.getProduct().getToken()) && Objects.equals(reqNote, normalizeNote(i.getNote())))
                .filter(i -> i.getStatuses().stream().noneMatch(
                                s -> "CANCELLED".equals(s.getToken())
                        )
                )
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Active dish with specified note not found in the order"));

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

        var orderEvent = WebSocketEvent.deleted(ORDER_ENTITY_TYPE, order.getToken());
        _notification.sendEventToTopic("/orders/updates", orderEvent);
    }

    @Override
    @Transactional
    public void addItemFromReservation(
            String waiterToken,
            String reservationToken,
            List<ReservationDishRequest> request
    ) {
        Orders order = _orderRepo.findByReservationToken(reservationToken)
                .filter(o -> o.getWaiter() != null && waiterToken.equals(o.getWaiter().getToken()))
                .orElseThrow(() -> new RuntimeException("Order not found or you are not the assigned waiter"));

        List<OrderItems> existingItems = _orderRepo.findItemsByOrderToken(order.getToken());
        List<String> requestedDishTokens = request.stream().map(ReservationDishRequest::getDishToken).toList();
        List<Dishes> allRequestedDishes = _dishRepo.listForOrder(requestedDishTokens);

        int addToPrice = 0;

        for (ReservationDishRequest r : request) {
            Dishes dish = allRequestedDishes.stream()
                    .filter(d -> r.getDishToken().equals(d.getToken()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Dish not found: " + r.getDishToken()));

            String reqNote = normalizeNote(r.getNote());

            Optional<OrderItems> existingItemOpt = existingItems.stream()
                    .filter(i -> dish.getToken().equals(i.getProduct().getToken()) &&
                            Objects.equals(reqNote, normalizeNote(i.getNote())))
                    .filter(i -> i.getStatuses().stream()
                            .noneMatch(s -> "CANCELLED".equals(s.getToken()))
                    )
                    .findFirst();

            if (existingItemOpt.isPresent()) {
                OrderItems item = existingItemOpt.get();
                item.setQuantity(item.getQuantity() + r.getQuantity());
                _orderRepo.saveItem(item);

                addToPrice += item.getPriceAtTimeOfOrder() * r.getQuantity();
            } else {
                OrderItems item = new OrderItems();
                item.setOrder(order);
                item.setProduct(dish);
                item.setQuantity(r.getQuantity());
                item.setPriceAtTimeOfOrder(dish.getPrice());
                item.setNote(reqNote);

                _orderRepo.saveItem(item);

                addToPrice += dish.getPrice() * r.getQuantity();
            }
        }

        order.setTotalPrice(order.getTotalPrice() + addToPrice);
        _orderRepo.save(order);

        List<OrderItems> updatedItems = _orderRepo.findItemsByOrderToken(order.getToken());
        sendOrderSyncEvents(order, updatedItems, false);
    }

    @Transactional
    @Override
    public void assignWaiterToOrders(String reservationToken, String waiterToken) {
        var order = _orderRepo.findByReservationToken(reservationToken)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found"));

        var waiter = _userRepo.findByToken(waiterToken);

        var orderStatus = _orderRepo.findStatusByToken("IN_PROGRESS");
        var orderItemsStatus = _orderRepo.findItemStatusByToken("IN_PROGRESS");

        order.setStatuses(new HashSet<>(Set.of(orderStatus)));
        order.setWaiter(waiter);

        List<OrderItems> items = _orderRepo.findItemsByOrderToken(order.getToken());

        for (OrderItems item : items) {
            boolean isPending = item.getStatuses()
                    .stream()
                    .anyMatch(s -> "PENDING".equals(s.getToken()));

            if (isPending || item.getStatuses().isEmpty()) item.setStatuses(new HashSet<>(Set.of(orderItemsStatus)));
        }

        _orderRepo.saveAllItems(items);
        _orderRepo.save(order);

    }


    @Override
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

            sendOrderSyncEvents(order, items, false);
        }
    }

    @Override
    @Cacheable(
            value = "orderStatuses",
            key = "T(org.springframework.context.i18n.LocaleContextHolder).getLocale().getLanguage()"
    )
    public DictionaryResponse getDictionary() {
        String lang = LocaleContextHolder.getLocale().getLanguage();
        return new DictionaryResponse(DictionaryHelper.map(_orderRepo.findAllStatuses(), lang));
    }

    @Override
    @Cacheable(
            value = "orderItemStatuses",
            key = "T(org.springframework.context.i18n.LocaleContextHolder).getLocale().getLanguage()"
    )
    public DictionaryResponse getItemStatusesDictionary() {
        String lang = LocaleContextHolder.getLocale().getLanguage();
        return new DictionaryResponse(DictionaryHelper.map(_orderRepo.findAllItemStatuses(), lang));
    }

    @Override
    @Transactional
    @Auditable(action = "ADD_ORDER_STATUS")
    @CacheEvict(value = "orderStatuses", allEntries = true)
    public void addStatus(AddEntityRequest request) {
        OrderStatus status = DictionaryHelper.createEntity(
                OrderStatus::new,
                request,
                _orderRepo::isStatusNameTaken,
                "Order status already exists"
        );

        _orderRepo.saveStatus(status);

        SyncDictionaryResponse payload = _syncMapper.toSyncDictionaryResponse(status);
        WebSocketEvent<SyncDictionaryResponse> event = WebSocketEvent.created(
                ORDER_STATUS_ENTITY_TYPE,
                status.getToken(),
                payload
        );
        _notification.sendEventToTopic("/dictionary/order-statuses", event);
    }

    @Override
    @Transactional
    @Auditable(action = "ADD_ORDER_ITEM_STATUS")
    @CacheEvict(value = "orderItemStatuses", allEntries = true)
    public void addItemStatus(AddEntityRequest request) {
        OrderItemsStatus status = DictionaryHelper.createEntity(
                OrderItemsStatus::new,
                request,
                _orderRepo::isItemStatusNameTaken,
                "Order item status already exists"
        );

        _orderRepo.saveItemStatus(status);

        SyncDictionaryResponse payload = _syncMapper.toSyncDictionaryResponse(status);
        WebSocketEvent<SyncDictionaryResponse> event = WebSocketEvent.created(
                ORDER_ITEM_STATUS_ENTITY_TYPE,
                status.getToken(),
                payload
        );
        _notification.sendEventToTopic("/dictionary/order-item-statuses", event);
    }

    @Override
    @Transactional
    @Auditable(action = "REMOVE_ORDER_STATUS")
    @CacheEvict(value = "orderStatuses", allEntries = true)
    public void removeStatus(String token) {
        DictionaryHelper.deleteEntity(
                token,
                _orderRepo::findStatusByToken,
                _orderRepo::saveStatus,
                statusToRemove -> {
                    OrderStatus fallbackStatus = _orderRepo.findStatusByToken("OTHER");

                    List<Orders> affectedOrders = _orderRepo.findOrdersByStatus(statusToRemove);

                    for (Orders order : affectedOrders) {
                        order.getStatuses().remove(statusToRemove);
                        order.getStatuses().add(fallbackStatus);
                        _orderRepo.save(order);
                    }
                }
        );

        WebSocketEvent<Void> event = WebSocketEvent.deleted(ORDER_STATUS_ENTITY_TYPE, token);
        _notification.sendEventToTopic("/dictionary/order-statuses", event);
    }

    @Override
    @Transactional
    @Auditable(action = "REMOVE_ORDER_ITEM_STATUS")
    @CacheEvict(value = "orderItemStatuses", allEntries = true)
    public void removeItemStatus(String token) {
        DictionaryHelper.deleteEntity(
                token,
                _orderRepo::findItemStatusByToken,
                _orderRepo::saveItemStatus,
                statusToRemove -> {
                    OrderItemsStatus fallbackStatus = _orderRepo.findItemStatusByToken("OTHER");

                    List<OrderItems> affectedItems = _orderRepo.findOrderItemsByStatus(statusToRemove);

                    for (OrderItems item : affectedItems) {
                        item.getStatuses().remove(statusToRemove);
                        item.getStatuses().add(fallbackStatus);
                        _orderRepo.saveItem(item);
                    }
                }
        );

        WebSocketEvent<Void> event = WebSocketEvent.deleted(ORDER_ITEM_STATUS_ENTITY_TYPE, token);
        _notification.sendEventToTopic("/dictionary/order-item-statuses", event);
    }

    private String normalizeNote(String note) {
        if (note == null || note.trim().isEmpty()) return note;
        return note.trim();
    }

    private void sendOrderSyncEvents(Orders order, List<OrderItems> items, boolean isNew) {
        WebSocketEvent<SyncOrderResponse> orderEvent = isNew
                ? WebSocketEvent.created(ORDER_ENTITY_TYPE, order.getToken(), _syncMapper.toSyncOrderResponse(order))
                : WebSocketEvent.updated(ORDER_ENTITY_TYPE, order.getToken(), _syncMapper.toSyncOrderResponse(order));

        _notification.sendEventToTopic("/orders/updates", orderEvent);

        for (OrderItems item : items) {
            WebSocketEvent<SyncOrderItemResponse> itemEvent = isNew
                    ? WebSocketEvent.created("ORDER_ITEM", item.getToken(), _syncMapper.toSyncOrderItemResponse(item))
                    : WebSocketEvent.updated("ORDER_ITEM", item.getToken(), _syncMapper.toSyncOrderItemResponse(item));

            _notification.sendEventToTopic("/orders/items", itemEvent);
        }
    }
}
