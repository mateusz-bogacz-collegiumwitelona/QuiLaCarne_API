package com.example.restaurant.services.reservation;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.request.ReservationDishRequest;
import com.example.restaurant.dto.request.ReservationRequest;
import com.example.restaurant.dto.response.ReservationDishResponse;
import com.example.restaurant.dto.response.ReservationResponse;
import com.example.restaurant.enums.ReservationStateEnum;
import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.fasade.interfaces.IOrderFacade;
import com.example.restaurant.models.Reservations;
import com.example.restaurant.models.lookup.ReservationStatus;
import com.example.restaurant.repository.interfaces.IReservationRepository;
import com.example.restaurant.repository.interfaces.ITableRespository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationCommandService {
  private final ITableRespository _tableRepo;
  private final IReservationRepository _reservationRepo;
  private final IUserRepository _userRepo;
  private final IOrderFacade _orderServices;
  private final ReservationSyncPublisher _syncPublisher;

  private static final String STATUS_ACTIVE = "ACTIVE";
  private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
  private static final String STATUS_NO_SHOW = "NO_SHOW";
  private static final String ROLE_WAITER = "ROLE_WAITER";

  @Transactional
  @Auditable(action = "CREATE_RESERVATION")
  public ReservationResponse create(ReservationRequest request, String userToken) {
    Duration duration = Duration.between(request.getStartTime(), request.getEndTime());

    if (duration.toMinutes() < 30)
      throw new IllegalStateException("Reservation must be at least 30 minutes long");

    if (duration.toHours() > 3)
      throw new IllegalStateException("Reservation cannot exceed 3 hours");

    if (!_tableRepo.isTableExist(request.getTableToken()))
      throw new EntityNotFoundException("Table not found");

    if (!_tableRepo.isTableAvailable(
        request.getTableToken(), request.getStartTime(), request.getEndTime()))
      throw new IllegalStateException("Table is already reserved for this time slot");

    var user = _userRepo.findByToken(userToken);
    var table = _tableRepo.findByToken(request.getTableToken());
    var activeStatus = _reservationRepo.findStatusByToken(STATUS_ACTIVE);

    Reservations reservation = new Reservations();
    reservation.setUser(user);
    reservation.setTableId(table);
    reservation.setStartTime(request.getStartTime());
    reservation.setEndTime(request.getEndTime());
    reservation.setReservationStatus(new HashSet<>(Set.of(activeStatus)));

    _reservationRepo.save(reservation);

    var orderCreate =
        _orderServices.createOrderForReservation(
            reservation.getToken(), request.getTableToken(), request.getDishes());

    ReservationResponse response = new ReservationResponse();

    response.setActive(true);

    response.setDishes(
        orderCreate.dishes().stream()
            .map(
                domainDish -> {
                  ReservationDishResponse dishRes = new ReservationDishResponse();
                  dishRes.setDishName(domainDish.dishName());
                  dishRes.setPrice(domainDish.price());
                  dishRes.setQuantity(domainDish.quantity());
                  return dishRes;
                })
            .toList());

    response.setTotalPrice(orderCreate.totalPrice());

    _syncPublisher.publishReservationCreate(reservation);

    return response;
  }

  @Transactional
  public void cancel(String reservationToken, String userToken) {
    Reservations reservation =
        _reservationRepo
            .findByTokenAndUserToken(reservationToken, userToken)
            .orElseThrow(() -> new EntityNotFoundException("Reservation not found"));

    ReservationStatus cancelledStatus = _reservationRepo.findStatusByToken("CANCELLED");
    ReservationStateEnum.from(reservation).cancel(reservation, cancelledStatus);

    _reservationRepo.save(reservation);

    _syncPublisher.publishReservationUpdated(reservation);
  }

  @Auditable(action = "REMOVE_ITEM_FROM_RESERVATION")
  public void removeItemFromReservation(
      String waiterToken, String reservationToken, ReservationDishRequest request) {
    _orderServices.removeItemFromReservation(waiterToken, reservationToken, request);
  }

  @Auditable(action = "ADD_ITEM_TO_RESERVATION")
  public void addItemFromReservation(
      String waiterToken, String reservationToken, List<ReservationDishRequest> request) {
    _orderServices.addItemFromReservation(waiterToken, reservationToken, request);
  }

  @Transactional
  @Auditable(action = "ASIGN_WAITER_TO_RESERVATION")
  public void assignWaiter(String reservationToken, String waiterToken) {
    if (!_userRepo.isInRole(ROLE_WAITER, waiterToken))
      throw new IllegalStateException(
          "Only users with WAITER role can be assigned to reservations");

    Reservations reservation =
        _reservationRepo
            .findByToken(reservationToken)
            .orElseThrow(() -> new EntityNotFoundException("Reservation not found"));

    ReservationStatus inProgressStatus = _reservationRepo.findStatusByToken(STATUS_IN_PROGRESS);
    ReservationStateEnum.from(reservation).assignWaiter(reservation, inProgressStatus);

    _reservationRepo.save(reservation);

    _orderServices.assignWaiterToOrders(reservationToken, waiterToken);

    _syncPublisher.publishReservationUpdated(reservation);
  }

  @Transactional
  @Auditable(action = "MARK_AS_ABSENT")
  public void isAbsent(String reservationToken) {
    Reservations reservation =
        _reservationRepo
            .findByToken(reservationToken)
            .orElseThrow(() -> new EntityNotFoundException("Reservation not found"));

    ReservationStatus noShowStatus = _reservationRepo.findStatusByToken(STATUS_NO_SHOW);
    ReservationStateEnum.from(reservation).markAsAbsent(reservation, noShowStatus);

    _reservationRepo.save(reservation);
    _orderServices.isAbsent(reservationToken);

    _syncPublisher.publishReservationUpdated(reservation);
  }
}
