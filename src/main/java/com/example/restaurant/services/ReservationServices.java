package com.example.restaurant.services;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.request.ClientReservationRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.request.ReservationDishRequest;
import com.example.restaurant.dto.request.ReservationRequest;
import com.example.restaurant.dto.response.*;
import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.helpers.DictionaryHelper;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.helpers.WebSocketEvent;
import com.example.restaurant.mappers.ReservationMapper;
import com.example.restaurant.mappers.SyncMapper;
import com.example.restaurant.models.Reservations;
import com.example.restaurant.models.lookup.ReservationStatus;
import com.example.restaurant.repository.interfaces.IReservationRepository;
import com.example.restaurant.repository.interfaces.ITableRespository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.services.interfaces.IOrderServices;
import com.example.restaurant.services.interfaces.IReservationServices;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReservationServices implements IReservationServices {
    private final ITableRespository _tableRepo;
    private final IReservationRepository _reservationRepo;
    private final IUserRepository _userRepo;
    private final IOrderServices _orderServices;
    private final ReservationMapper _reservationMapper;
    private final NotificationServices _notification;

    private final SyncMapper _syncMapper;

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_NO_SHOW = "NO_SHOW";
    private static final String ROLE_WAITER = "ROLE_WAITER";

    private static final String RESERVATION_ENTITY_TYPE = "RESERVATION";

    @Override
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

        if (!_tableRepo.isTableAvailable(request.getTableToken(), request.getStartTime(), request.getEndTime()))
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

        var orderCreate = _orderServices.createOrderForReservation(
                reservation.getToken(),
                request.getTableToken(),
                request.getDishes());

        ReservationResponse response = new ReservationResponse();
        response.setActive(true);
        response.setDishes(orderCreate.dishes().stream().map(domainDish -> {
            ReservationDishResponse dishRes = new ReservationDishResponse();
            dishRes.setDishName(domainDish.dishName());
            dishRes.setPrice(domainDish.price());
            dishRes.setQuantity(domainDish.quantity());
            return dishRes;
        }).toList());
        response.setTotalPrice(orderCreate.totalPrice());

        WebSocketEvent<SyncReservationResponse> event = WebSocketEvent.created(
                RESERVATION_ENTITY_TYPE,
                reservation.getToken(),
                _syncMapper.toSyncReservationResponse(reservation)
        );
        _notification.sendEventToTopic("/reservations/updates", event);

        return response;
    }

    @Override
    public PagedResult<ClientReservationResponse> history(
            ClientReservationRequest request,
            PaggedRequest pagged, String
                    userToken) {
        String lang = LocaleContextHolder.getLocale().getLanguage();
        Pageable pageable = PageRequest.of(
                Math.max(0, pagged.getPage() - 1),
                pagged.getSize(),
                Sort.by(Sort.Direction.ASC, "startTime")
        );

        Specification<Reservations> spec = (
                root,
                query,
                criteriaBuilder
        ) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("user").get("token"), userToken));

            if (request.getFromDate() != null)
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("startTime"), request.getFromDate()));

            if (request.getToDate() != null)
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("startTime"), request.getToDate()));

            if (request.getStatusToken() != null && !request.getStatusToken().isEmpty()) {
                Join<Reservations, ReservationStatus> statusJoin = root.join("reservationStatus");
                predicates.add(criteriaBuilder.equal(statusJoin.get("token"), request.getStatusToken()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<Reservations> page = _reservationRepo.findAll(spec, pageable);

        return new PagedResult<>(page.map(r ->
                _reservationMapper.toClientReservationResponse(r, lang)
        ));
    }

    @Override
    public ReservationDetailsResponse details(String reservationToken, String userToken) {
        return _reservationRepo.findByTokenAndUserToken(reservationToken, userToken)
                .map(
                        r -> _reservationMapper
                                .toReservationDetailsResponse(r, LocaleContextHolder.getLocale().getLanguage())
                )
                .orElse(null);
    }

    @Transactional
    @Override
    public void cancel(String reservationToken, String userToken) {
        Reservations reservation = _reservationRepo.findByTokenAndUserToken(reservationToken, userToken)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found"));

        ReservationStatus cancelledStatus = _reservationRepo.findStatusByToken("CANCELLED");
        reservation.setReservationStatus(new HashSet<>(Set.of(cancelledStatus)));

        _reservationRepo.save(reservation);

        WebSocketEvent<SyncReservationResponse> event = WebSocketEvent.updated(
                RESERVATION_ENTITY_TYPE,
                reservation.getToken(),
                _syncMapper.toSyncReservationResponse(reservation)
        );
        _notification.sendEventToTopic("/reservations/updates", event);
    }

    @Auditable(action = "REMOVE_ITEM_FROM_RESERVATION")
    @Override
    public void removeItemFromReservation(
            String waiterToken,
            String reservationToken,
            ReservationDishRequest request
    ) {
        _orderServices.removeItemFromReservation(waiterToken, reservationToken, request);
    }

    @Auditable(action = "ADD_ITEM_TO_RESERVATION")
    @Override
    public void addItemFromReservation(
            String waiterToken,
            String reservationToken,
            List<ReservationDishRequest> request
    ) {
        _orderServices.addItemFromReservation(waiterToken, reservationToken, request);
    }

    @Transactional
    @Auditable(action = "ASIGN_WAITER_TO_RESERVATION")
    @Override
    public void assignWaiter(String reservationToken, String waiterToken) {
        if (!_userRepo.isInRole(ROLE_WAITER, waiterToken))
            throw new IllegalStateException("Only users with WAITER role can be assigned to reservations");

        Reservations reservation = _reservationRepo.findByToken(reservationToken)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found"));

        ReservationStatus inProgressStatus = _reservationRepo.findStatusByToken(STATUS_IN_PROGRESS);
        reservation.setReservationStatus(new HashSet<>(Set.of(inProgressStatus)));
        _reservationRepo.save(reservation);

        _orderServices.assignWaiterToOrders(reservationToken, waiterToken);

        WebSocketEvent<SyncReservationResponse> event = WebSocketEvent.updated(
                RESERVATION_ENTITY_TYPE,
                reservation.getToken(),
                _syncMapper.toSyncReservationResponse(reservation)
        );
        _notification.sendEventToTopic("/reservations/updates", event);
    }

    @Transactional
    @Override
    @Auditable(action = "MARK_AS_ABSENT")
    public void isAbsent(String reservationToken) {
        Reservations reservation = _reservationRepo.findByToken(reservationToken)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found"));

        boolean isActive = reservation.getReservationStatus().stream()
                .anyMatch(status -> status.getToken().equals(STATUS_ACTIVE));

        if (!isActive) throw new IllegalStateException("Only ACTIVE reservations can be set to NO_SHOW");

        reservation.setReservationStatus(Set.of(_reservationRepo.findStatusByToken(STATUS_NO_SHOW)));
        _reservationRepo.save(reservation);
        _orderServices.isAbsent(reservationToken);

        WebSocketEvent<SyncReservationResponse> event = WebSocketEvent.updated(
                RESERVATION_ENTITY_TYPE,
                reservation.getToken(),
                _syncMapper.toSyncReservationResponse(reservation)
        );
        _notification.sendEventToTopic("/reservations/updates", event);
    }

    @Override
    @Cacheable(
            value = "reservationStatuses",
            key = "T(org.springframework.context.i18n.LocaleContextHolder).getLocale().getLanguage()"
    )
    public DictionaryResponse getDictionary() {
        String lang = LocaleContextHolder.getLocale().getLanguage();
        return new DictionaryResponse(DictionaryHelper.map(_reservationRepo.findAllStatuses(), lang));
    }
}
