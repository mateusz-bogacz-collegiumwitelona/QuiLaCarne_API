package com.example.restaurant.fasade.interfaces;

import com.example.restaurant.dto.domain.OrderSummaryDomain;
import com.example.restaurant.dto.domain.ReservationDomain;
import com.example.restaurant.dto.domain.TodayOrderSummaryDomain;
import com.example.restaurant.dto.request.AddEntityRequest;
import com.example.restaurant.dto.request.ReservationDishRequest;
import com.example.restaurant.dto.response.DictionaryResponse;
import java.util.List;

public interface IOrderFacade {
  ReservationDomain createOrderForReservation(
      String reservationToken, String tableToken, List<ReservationDishRequest> dishesRequest);

  OrderSummaryDomain getOrderSummaryForReservation(String reservationToken);

  TodayOrderSummaryDomain todayOrderDetails(String reservationToken, String lang);

  void removeItemFromReservation(
      String waiterToken, String reservationToken, ReservationDishRequest request);

  void addItemFromReservation(
      String waiterToken, String reservationToken, List<ReservationDishRequest> request);

  void assignWaiterToOrders(String reservationToken, String waiterToken);

  void isAbsent(String reservationToken);

  DictionaryResponse getDictionary();

  DictionaryResponse getItemStatusesDictionary();

  void addStatus(AddEntityRequest request);

  void addItemStatus(AddEntityRequest request);

  void removeStatus(String token);

  void removeItemStatus(String token);
}
