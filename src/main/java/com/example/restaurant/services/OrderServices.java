package com.example.restaurant.services;

import com.example.restaurant.dto.domain.ReservationDishDoamin;
import com.example.restaurant.dto.domain.ReservationDomain;
import com.example.restaurant.dto.request.ReservationDishRequest;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.models.OrderItems;
import com.example.restaurant.models.Orders;
import com.example.restaurant.repository.interfaces.IDishRepository;
import com.example.restaurant.repository.interfaces.IOrderRepository;
import com.example.restaurant.repository.interfaces.IReservationRepository;
import com.example.restaurant.repository.interfaces.ITableRespository;
import com.example.restaurant.services.interfaces.IOrderServices;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServices implements IOrderServices {
    private final IOrderRepository _orderRepo;
    private final IDishRepository _dishRepo;
    private final IReservationRepository _reservationRepo;
    private final ITableRespository _tableRepo;


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
                .collect(Collectors.toList());

        Map<String, Dishes> dishesMap = _dishRepo.listForOrder(dishTokens).stream()
                .collect(Collectors.toMap(Dishes::getToken, dish -> dish));

        var reservation = _reservationRepo.findByToken(reservationToken);

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
                throw new RuntimeException("Dish not found: " + req.getDishToken());

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
        return new ReservationDomain(reservationDishes, totalPrice);
    }
}
