package com.example.restaurant.services.reservation;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.request.ReservationDishRequest;
import com.example.restaurant.dto.request.ReservationRequest;
import com.example.restaurant.dto.response.ReservationDishResponse;
import com.example.restaurant.dto.response.ReservationResponse;
import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.fasade.interfaces.IOrderFacade;
import com.example.restaurant.helpers.staics.RoleType;
import com.example.restaurant.models.Reservations;
import com.example.restaurant.models.lookup.ReservationStatus;
import com.example.restaurant.repository.interfaces.IReservationRepository;
import com.example.restaurant.repository.interfaces.ITableRespository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.state.ReservationStateLogic;
import com.example.restaurant.validators.reservation.ReservationCreateValidator;
import jakarta.transaction.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationCommandService {
  private final ITableRespository _tableRepo;
  private final IReservationRepository _reservationRepo;
  private final IUserRepository _userRepo;
  private final IOrderFacade _orderServices;
  private final ReservationSyncPublisher _syncPublisher;

  private final List<ReservationCreateValidator> _validators;

  private static final String STATUS_ACTIVE = "ACTIVE";
  private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
  private static final String STATUS_NO_SHOW = "NO_SHOW";

  @Transactional
  @Auditable(action = "CREATE_RESERVATION")
  public ReservationResponse create(ReservationRequest request, String userToken) {
    for (ReservationCreateValidator validator : _validators) {
      validator.validate(request);
    }

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

    log.info("Created reservation {}", reservation.getToken());

    return response;
  }

  @Transactional
  public void cancel(String reservationToken, String userToken) {
    Reservations reservation =
        _reservationRepo
            .findByTokenAndUserToken(reservationToken, userToken)
            .orElseThrow(() -> new EntityNotFoundException("Reservation not found"));

    ReservationStatus cancelledStatus = _reservationRepo.findStatusByToken("CANCELLED");
    ReservationStateLogic.from(reservation).cancel(reservation, cancelledStatus);

    _reservationRepo.save(reservation);

    log.info("Cancelled reservation {}", reservation.getToken());

    _syncPublisher.publishReservationUpdated(reservation);
  }

  @Auditable(action = "REMOVE_ITEM_FROM_RESERVATION")
  public void removeItemFromReservation(
      String waiterToken, String reservationToken, ReservationDishRequest request) {
    _orderServices.removeItemFromReservation(waiterToken, reservationToken, request);
    log.info("Removed from reservation {} item {}", reservationToken, request.getDishToken());
  }

  @Auditable(action = "ADD_ITEM_TO_RESERVATION")
  public void addItemFromReservation(
      String waiterToken, String reservationToken, List<ReservationDishRequest> request) {
    _orderServices.addItemFromReservation(waiterToken, reservationToken, request);
    log.info("Added from reservation {} items {}", reservationToken, List.of(request));
  }

  @Transactional
  @Auditable(action = "ASIGN_WAITER_TO_RESERVATION")
  public void assignWaiter(String reservationToken, String waiterToken) {
    if (!_userRepo.isInRole(RoleType.ROLE_WAITER, waiterToken))
      throw new IllegalStateException(
          "Only users with WAITER role can be assigned to reservations");

    Reservations reservation =
        _reservationRepo
            .findByToken(reservationToken)
            .orElseThrow(() -> new EntityNotFoundException("Reservation not found"));

    ReservationStatus inProgressStatus = _reservationRepo.findStatusByToken(STATUS_IN_PROGRESS);
    ReservationStateLogic.from(reservation).assignWaiter(reservation, inProgressStatus);

    _reservationRepo.save(reservation);

    _orderServices.assignWaiterToOrders(reservationToken, waiterToken);

    _syncPublisher.publishReservationUpdated(reservation);
    log.info("Assigned waiter {} to reservation {}", waiterToken, reservationToken);
  }

  @Transactional
  @Auditable(action = "MARK_AS_ABSENT")
  public void isAbsent(String reservationToken) {
    Reservations reservation =
        _reservationRepo
            .findByToken(reservationToken)
            .orElseThrow(() -> new EntityNotFoundException("Reservation not found"));

    ReservationStatus noShowStatus = _reservationRepo.findStatusByToken(STATUS_NO_SHOW);
    ReservationStateLogic.from(reservation).markAsAbsent(reservation, noShowStatus);

    _reservationRepo.save(reservation);
    _orderServices.isAbsent(reservationToken);

    _syncPublisher.publishReservationUpdated(reservation);
    log.info("Absent reservation {}", reservationToken);
  }
}
