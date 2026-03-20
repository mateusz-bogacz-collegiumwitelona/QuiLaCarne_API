package com.example.restaurant.repository;

import com.example.restaurant.dto.domain.OrderSummaryDomain;
import com.example.restaurant.dto.domain.ReservationDishDoamin;
import com.example.restaurant.dto.domain.ReservationDomain;
import com.example.restaurant.dto.domain.TodayOrderSummaryDomain;
import com.example.restaurant.dto.request.ReservationDishRequest;
import com.example.restaurant.dto.response.ReservationDishResponse;
import com.example.restaurant.dto.response.TodayReservationDishResponse;
import com.example.restaurant.exceptions.ReservationNotFoundException;
import com.example.restaurant.models.*;
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
    public boolean removeItemFromReservation(String userToken, String reservationToken, ReservationDishRequest request) {
        _jpaReservationsRepo.findByTokenAndUser_Token(reservationToken, userToken)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found or access denied"));

        Orders order = _jpaOrderRepo.findByReservation_Token(reservationToken)
                .orElseThrow(() -> new RuntimeException("Order not found"));

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

    private String normalizeNote(String note) {
        if (note == null || note.trim().isEmpty()) return note;
        return note.trim();
    }
}
