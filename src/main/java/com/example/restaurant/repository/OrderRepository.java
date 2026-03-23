package com.example.restaurant.repository;

import com.example.restaurant.dto.domain.OrderSummaryDomain;
import com.example.restaurant.dto.domain.ReservationDishDoamin;
import com.example.restaurant.dto.domain.ReservationDomain;
import com.example.restaurant.dto.domain.TodayOrderSummaryDomain;
import com.example.restaurant.dto.request.ReservationDishRequest;
import com.example.restaurant.dto.response.ReservationDishResponse;
import com.example.restaurant.dto.response.TodayReservationDishResponse;
import com.example.restaurant.exceptions.ReservationNotFoundException;
import com.example.restaurant.exceptions.UserNotFoundException;
import com.example.restaurant.models.*;
import com.example.restaurant.models.lookup.OrderItemsStatus;
import com.example.restaurant.models.lookup.OrderStatus;
import com.example.restaurant.repository.interfaces.IOrderRepository;
import com.example.restaurant.repository.interfaces.jpa.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class OrderRepository implements IOrderRepository {
    private final IJpaDishRepository _jpaDishRepo;
    private final IJpaOrderRepository _jpaOrderRepo;
    private final IJpaOrderItemsRepository _jpaOrderItemRepo;
    private final IJpaOrederStatusRepositry _jpaOrderStatusRepo;
    private final IJpaReservationsRepository _jpaReservationsRepo;
    private final IJpaTableRepository _jpaTableRepo;
    private final IJpaOrderItemStatusRepository _jpaOrderItemStatusRepo;
    private final IJpaUserRepository _jpaUserRepo;

    @Override
    @Transactional
    public ReservationDomain createOrderForReservation(String reservationToken, String tableToken, List<ReservationDishRequest> dishesRequest) {
        Reservations reservation = _jpaReservationsRepo.findByToken(reservationToken)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        RestaurantTables table = _jpaTableRepo.findByToken(tableToken);

        List<String> dishTokens = dishesRequest.stream().map(ReservationDishRequest::getDishToken).toList();
        List<Dishes> fetchedDishes = _jpaDishRepo.findAllByTokenIn(dishTokens);

        Orders order = new Orders();
        order.setReservation(reservation);
        order.setTable(table);

        OrderStatus pendingStatus = _jpaOrderStatusRepo.findByToken("PENDING")
                .orElseThrow(() -> new RuntimeException("Order status not found"));
        order.setStatuses(Set.of(pendingStatus));

        int totalPrices = 0;
        List<OrderItems> orderItems = new ArrayList<>();
        List<ReservationDishDoamin> dishesDomain = new ArrayList<>();

        for (ReservationDishRequest req : dishesRequest) {
            Dishes dish = fetchedDishes.stream()
                    .filter(d -> d.getToken().equals(req.getDishToken()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Dish not found"));

            OrderItems item = new OrderItems();
            item.setOrder(order);
            item.setProduct(dish);
            item.setQuantity(req.getQuantity());
            item.setPriceAtTimeOfOrder(dish.getPrice());
            item.setNote(normalizeNote(req.getNote()));

            totalPrices += (dish.getPrice() * req.getQuantity());
            orderItems.add(item);

            dishesDomain.add(new ReservationDishDoamin(
                    dish.getName(),
                    dish.getPrice(),
                    req.getQuantity()
            ));
        }

        order.setTotalPrice(totalPrices);

        _jpaOrderRepo.saveAndFlush(order);
        _jpaOrderItemRepo.saveAllAndFlush(orderItems);

        return new ReservationDomain(dishesDomain, totalPrices);
    }

    @Override
    public OrderSummaryDomain getOrderSummaryForReservation(String reservationToken) {
        Optional<Orders> orderOpt = _jpaOrderRepo.findByReservation_Token(reservationToken);

        if (orderOpt.isEmpty()) return new OrderSummaryDomain(0, List.of());

        Orders order = orderOpt.get();

        List<OrderItems> items = _jpaOrderItemRepo.findAllByOrder_Token(order.getToken());

        List<ReservationDishResponse> dishes = items.stream().map(item -> {
            ReservationDishResponse dish = new ReservationDishResponse();
            dish.setDishName(item.getProduct().getName());
            dish.setPrice(item.getPriceAtTimeOfOrder());
            dish.setQuantity(item.getQuantity());
            return dish;
        }).toList();

        return new OrderSummaryDomain(order.getTotalPrice(), dishes);
    }

    @Override
    public TodayOrderSummaryDomain todayOrderDetails(String reservationToken, String lang) {
        var orderOpt = _jpaOrderRepo.findByReservation_Token(reservationToken);

        if (orderOpt.isEmpty()) return new TodayOrderSummaryDomain(0, List.of());

        var order = orderOpt.get();

        var items = _jpaOrderItemRepo.findAllByOrder_Token(order.getToken());

        List<TodayReservationDishResponse> dishResponses = items.stream().map(item -> {
            var dish = item.getProduct();
            TodayReservationDishResponse dto = new TodayReservationDishResponse();

            dto.setDishToken(dish.getToken());
            dto.setDishName(dish.getName());
            dto.setPrice(dish.getPrice());
            dto.setQuantity(item.getQuantity());
            dto.setNote(item.getNote());

            List<String> ingredients = dish.getIngredients().stream()
                    .map(ing -> "pl".equalsIgnoreCase(lang) ? ing.getNamePl() : ing.getNameEn())
                    .toList();

            dto.setIngredient(ingredients);

            List<String> allergens = dish.getIngredients().stream()
                    .flatMap(ing -> ing.getAllergens().stream())
                    .map(al -> "pl".equalsIgnoreCase(lang) ? al.getNamePl() : al.getNameEn())
                    .distinct()
                    .toList();

            dto.setAllergens(allergens);

            return dto;
        }).toList();

        return new TodayOrderSummaryDomain(order.getTotalPrice(), dishResponses);
    }

    @Override
    @Transactional
    public boolean removeItemFromReservation(String waiterToken, String reservationToken, ReservationDishRequest request) {
        Orders order = _jpaOrderRepo.findByReservation_Token(reservationToken)
                .filter(o -> o.getWaiter() != null && o.getWaiter().getToken().equals(waiterToken))
                .orElseThrow(() -> new RuntimeException("Order not found or you are not the assigned waiter"));

        List<OrderItems> items = _jpaOrderItemRepo.findAllByOrder_Token(order.getToken());

        String reqNote = normalizeNote(request.getNote());

        OrderItems itemToMod = items.stream()
                .filter(i -> i.getProduct().getToken().equals(request.getDishToken()) && Objects.equals(reqNote, normalizeNote(i.getNote())))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Dish with specified note not found in the order"));

        int currentQuantity = itemToMod.getQuantity();
        int quantityToRemove = request.getQuantity();
        int pricePerItem = itemToMod.getPriceAtTimeOfOrder();

        if (quantityToRemove >= currentQuantity) {
            order.setTotalPrice(order.getTotalPrice() - (pricePerItem * currentQuantity));
            _jpaOrderItemRepo.delete(itemToMod);
        } else {
            itemToMod.setQuantity(currentQuantity - quantityToRemove);
            order.setTotalPrice(order.getTotalPrice() - (pricePerItem * quantityToRemove));
            _jpaOrderItemRepo.save(itemToMod);
        }

        _jpaOrderRepo.saveAndFlush(order);

        return true;
    }

    @Override
    @Transactional
    public boolean addItemFromReservation(String waiterToken, String reservationToken, List<ReservationDishRequest> request) {
        Orders order = _jpaOrderRepo.findByReservation_Token(reservationToken)
                .filter(o -> o.getWaiter() != null && o.getWaiter().getToken().equals(waiterToken))
                .orElseThrow(() -> new RuntimeException("Order not found or you are not the assigned waiter"));

        List<OrderItems> existingItems = _jpaOrderItemRepo.findAllByOrder_Token(order.getToken());
        List<String> requestedDishTokens = request.stream().map(ReservationDishRequest::getDishToken).toList();
        List<Dishes> allRequestedDishes = _jpaDishRepo.findAllByTokenIn(requestedDishTokens);

        int addToPrice = 0;

        for (ReservationDishRequest r : request) {
            Dishes dish = allRequestedDishes.stream()
                    .filter(d -> d.getToken().equals(r.getDishToken()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Dish not found: " + r.getDishToken()));

            String reqNote = normalizeNote(r.getNote());

            Optional<OrderItems> existingItemOpt = existingItems.stream()
                    .filter(i -> i.getProduct().getToken().equals(dish.getToken()) &&
                            java.util.Objects.equals(reqNote, normalizeNote(i.getNote())))
                    .findFirst();

            if (existingItemOpt.isPresent()) {
                OrderItems item = existingItemOpt.get();
                item.setQuantity(item.getQuantity() + r.getQuantity());
                _jpaOrderItemRepo.save(item);

                addToPrice += item.getPriceAtTimeOfOrder() * r.getQuantity();
            } else {
                OrderItems item = new OrderItems();
                item.setOrder(order);
                item.setProduct(dish);
                item.setQuantity(r.getQuantity());
                item.setPriceAtTimeOfOrder(dish.getPrice());
                item.setNote(reqNote);

                _jpaOrderItemRepo.save(item);

                addToPrice += dish.getPrice() * r.getQuantity();
            }
        }

        order.setTotalPrice(order.getTotalPrice() + addToPrice);
        _jpaOrderRepo.saveAndFlush(order);

        return true;
    }

    private String normalizeNote(String note) {
        if (note == null || note.trim().isEmpty()) return note;
        return note.trim();
    }

    @Transactional
    @Override
    public boolean assignWaiterToOrders(String reservationToken, String waiterToken) {
        Orders order = _jpaOrderRepo.findByReservation_Token(reservationToken)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        Users waiter = _jpaUserRepo.findByToken(waiterToken).orElseThrow(() -> new UserNotFoundException("Waiter not found"));

        OrderStatus orderStatus = _jpaOrderStatusRepo.findByToken("IN_PROGRESS").orElseThrow(
                () -> new RuntimeException("Order status not found"));

        OrderItemsStatus orderItemsStatus = _jpaOrderItemStatusRepo.findByToken("IN_PROGRESS").orElseThrow(
                () -> new RuntimeException("Order item status not found"));

        order.setStatuses(Set.of(orderStatus));
        order.setWaiter(waiter);

        List<OrderItems> items = _jpaOrderItemRepo.findAllByOrder_Token(order.getToken());

        for (OrderItems item : items) {
            boolean isPending = item.getStatuses()
                    .stream()
                    .anyMatch(s -> s.getToken().equals("PENDING"));

            if (isPending || item.getStatuses().isEmpty()) item.setStatuses(Set.of(orderItemsStatus));
        }

        _jpaOrderItemRepo.saveAll(items);
        _jpaOrderRepo.saveAndFlush(order);

        return true;
    }

    @Override
    public boolean isAbsent(String reservationToken) {
        Optional<Orders> orderOpt = _jpaOrderRepo.findByReservation_Token(reservationToken);

        if (orderOpt.isEmpty()) {
            return true;
        }

        Orders order = orderOpt.get();

        OrderStatus orderStatus = _jpaOrderStatusRepo.findByToken("CANCELLED").orElseThrow(
                () -> new RuntimeException("Order status not found"));

        OrderItemsStatus orderItemsStatus = _jpaOrderItemStatusRepo.findByToken("CANCELLED").orElseThrow(
                () -> new RuntimeException("Order item status not found"));

        order.setStatuses(Set.of(orderStatus));

        List<OrderItems> items = _jpaOrderItemRepo.findAllByOrder_Token(order.getToken());
        for (OrderItems item : items) {
            item.setStatuses(Set.of(orderItemsStatus));
        }

        _jpaOrderItemRepo.saveAll(items);
        _jpaOrderRepo.saveAndFlush(order);

        return true;
    }
}
