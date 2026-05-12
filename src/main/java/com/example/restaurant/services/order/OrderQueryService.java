package com.example.restaurant.services.order;

import com.example.restaurant.dto.domain.OrderSummaryDomain;
import com.example.restaurant.dto.domain.TodayOrderSummaryDomain;
import com.example.restaurant.dto.response.ReservationDishResponse;
import com.example.restaurant.dto.response.TodayReservationDishResponse;
import com.example.restaurant.models.OrderItems;
import com.example.restaurant.models.Orders;
import com.example.restaurant.repository.interfaces.IOrderRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderQueryService {
  private final IOrderRepository _orderRepo;

  public OrderSummaryDomain getOrderSummaryForReservation(String reservationToken) {
    var orderOpt = _orderRepo.findByReservationToken(reservationToken);

    if (orderOpt.isEmpty()) return new OrderSummaryDomain(0, new ArrayList<>());

    Orders order = orderOpt.get();
    List<OrderItems> items = _orderRepo.findItemsByOrderToken(order.getToken());

    String lang = LocaleContextHolder.getLocale().getLanguage();

    List<ReservationDishResponse> dishResponses =
        items.stream()
            .map(
                item -> {
                  ReservationDishResponse response = new ReservationDishResponse();
                  response.setDishName(item.getProduct().getName());
                  response.setQuantity(item.getQuantity());
                  response.setPrice(item.getPriceAtTimeOfOrder());

                  if (item.getStatuses() != null && !item.getStatuses().isEmpty()) {
                    response.setStatus(item.getStatuses().iterator().next().translate(lang));
                  } else {
                    response.setStatus("UNKNOWN");
                  }

                  return response;
                })
            .toList();

    int totalPrice =
        items.stream().mapToInt(item -> item.getPriceAtTimeOfOrder() * item.getQuantity()).sum();

    return new OrderSummaryDomain(totalPrice, dishResponses);
  }

  public TodayOrderSummaryDomain todayOrderDetails(String reservationToken, String lang) {
    var orderOpt = _orderRepo.findByReservationToken(reservationToken);

    if (orderOpt.isEmpty()) return new TodayOrderSummaryDomain(0, new ArrayList<>());

    Orders order = orderOpt.get();
    List<OrderItems> items = _orderRepo.findItemsByOrderToken(order.getToken());

    List<TodayReservationDishResponse> dishResponses =
        items.stream()
            .map(
                item -> {
                  TodayReservationDishResponse response = new TodayReservationDishResponse();
                  response.setDishName(item.getProduct().getName());
                  response.setQuantity(item.getQuantity());
                  response.setPrice(item.getPriceAtTimeOfOrder());
                  response.setNote(item.getNote() != null ? item.getNote() : "");

                  if (item.getProduct().getIngredients() != null) {
                    List<String> ingredients =
                        item.getProduct().getIngredients().stream()
                            .map(i -> i.translate(lang))
                            .toList();

                    List<String> allergens =
                        item.getProduct().getIngredients().stream()
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
                })
            .toList();

    int totalPrice =
        items.stream().mapToInt(item -> item.getPriceAtTimeOfOrder() * item.getQuantity()).sum();

    return new TodayOrderSummaryDomain(totalPrice, dishResponses);
  }
}
