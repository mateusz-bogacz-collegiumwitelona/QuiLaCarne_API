package com.example.restaurant.fasade;

import com.example.restaurant.dto.request.ClientReservationRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.request.ReservationDishRequest;
import com.example.restaurant.dto.request.ReservationRequest;
import com.example.restaurant.dto.response.*;
import com.example.restaurant.fasade.interfaces.IReservationFacade;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.services.reservation.ReservationCommandService;
import com.example.restaurant.services.reservation.ReservationDictionaryService;
import com.example.restaurant.services.reservation.ReservationQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationFacade implements IReservationFacade {
  private final ReservationCommandService _commandService;
  private final ReservationDictionaryService _dictionaryService;
  private final ReservationQueryService _queryService;

  @Override
  public ReservationResponse create(ReservationRequest request, String userToken) {
    return _commandService.create(request, userToken);
  }

  @Override
  public PagedResult<ClientReservationResponse> history(
      ClientReservationRequest request, PaggedRequest pagged, String userToken) {
    return _queryService.history(request, pagged, userToken);
  }

  @Override
  public ReservationDetailsResponse details(String reservationToken, String userToken) {
    return _queryService.details(reservationToken, userToken);
  }

  @Override
  public void cancel(String reservationToken, String userToken) {
    _commandService.cancel(reservationToken, userToken);
  }

  @Override
  public void addItemFromReservation(
      String waiterToken, String reservationToken, List<ReservationDishRequest> request) {
    _commandService.addItemFromReservation(waiterToken, reservationToken, request);
  }

  @Override
  public void removeItemFromReservation(
      String waiterToken, String reservationToken, ReservationDishRequest request) {
    _commandService.removeItemFromReservation(waiterToken, reservationToken, request);
  }

  @Override
  public void assignWaiter(String reservationToken, String waiterToken) {
    _commandService.assignWaiter(reservationToken, waiterToken);
  }

  @Override
  public void isAbsent(String reservationToken) {
    _commandService.isAbsent(reservationToken);
  }

  @Override
  public DictionaryResponse getDictionary() {
    return _dictionaryService.getDictionary();
  }

  @Override
  public void markAsComplete(String reservationToken) {
    _commandService.markAsComplete(reservationToken);
  }
}
