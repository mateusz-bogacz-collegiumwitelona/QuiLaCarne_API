package com.example.restaurant.repository;

import com.example.restaurant.dto.domain.ReservationDishDoamin;
import com.example.restaurant.dto.domain.ReservationDomain;
import com.example.restaurant.dto.request.ReservationDishRequest;
import com.example.restaurant.models.*;
import com.example.restaurant.models.lookup.OrderStatus;
import com.example.restaurant.repository.interfaces.IOrderRepository;
import com.example.restaurant.repository.interfaces.jpa.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
            item.setNote(req.getNote());

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
}
