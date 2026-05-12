package com.example.restaurant.fasade;

import com.example.restaurant.dto.domain.OrderSummaryDomain;
import com.example.restaurant.dto.domain.ReservationDomain;
import com.example.restaurant.dto.domain.TodayOrderSummaryDomain;
import com.example.restaurant.dto.request.AddEntityRequest;
import com.example.restaurant.dto.request.ReservationDishRequest;
import com.example.restaurant.dto.response.DictionaryResponse;
import com.example.restaurant.fasade.interfaces.IOrderFacade;
import com.example.restaurant.services.order.OrderDictionaryService;
import com.example.restaurant.services.order.OrderQueryService;
import com.example.restaurant.services.order.OrderWorkflowService;
import jakarta.transaction.Transactional;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderFacade implements IOrderFacade {
  private final OrderWorkflowService _orderWorkflow;
  private final OrderQueryService _orderQuery;
  private final OrderDictionaryService _orderDictionary;

  @Override
  public ReservationDomain createOrderForReservation(
      String reservationToken, String tableToken, List<ReservationDishRequest> dishesRequest) {
    return _orderWorkflow.createOrderForReservation(reservationToken, tableToken, dishesRequest);
  }

  @Override
  public OrderSummaryDomain getOrderSummaryForReservation(String reservationToken) {
    return _orderQuery.getOrderSummaryForReservation(reservationToken);
  }

  @Override
  public TodayOrderSummaryDomain todayOrderDetails(String reservationToken, String lang) {
    return _orderQuery.todayOrderDetails(reservationToken, lang);
  }

  @Override
  public void removeItemFromReservation(
      String waiterToken, String reservationToken, ReservationDishRequest request) {
    _orderWorkflow.removeItemFromReservation(waiterToken, reservationToken, request);
  }

  @Override
  @Transactional
  public void addItemFromReservation(
      String waiterToken, String reservationToken, List<ReservationDishRequest> request) {
    _orderWorkflow.addItemFromReservation(waiterToken, reservationToken, request);
  }

  @Transactional
  @Override
  public void assignWaiterToOrders(String reservationToken, String waiterToken) {
    _orderWorkflow.assignWaiterToOrders(reservationToken, waiterToken);
  }

  @Override
  public void isAbsent(String reservationToken) {
    _orderWorkflow.isAbsent(reservationToken);
  }

  @Override
  public DictionaryResponse getDictionary() {
    return _orderDictionary.getDictionary();
  }

  @Override
  public DictionaryResponse getItemStatusesDictionary() {
    return _orderDictionary.getItemStatusesDictionary();
  }

  @Override
  public void addStatus(AddEntityRequest request) {
    _orderDictionary.addStatus(request);
  }

  @Override
  public void addItemStatus(AddEntityRequest request) {
    _orderDictionary.addItemStatus(request);
  }

  @Override
  public void removeStatus(String token) {
    _orderDictionary.removeStatus(token);
  }

  @Override
  public void removeItemStatus(String token) {
    _orderDictionary.removeItemStatus(token);
  }
}
