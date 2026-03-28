package com.example.restaurant.services;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.request.ClientReservationRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.request.ReservationDishRequest;
import com.example.restaurant.dto.request.ReservationRequest;
import com.example.restaurant.dto.response.*;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.repository.interfaces.IOrderRepository;
import com.example.restaurant.repository.interfaces.IReservationRepository;
import com.example.restaurant.repository.interfaces.ITableRespository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.services.interfaces.IOrderServices;
import com.example.restaurant.services.interfaces.IReservationServices;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationServices implements IReservationServices {
    private final ITableRespository _tableRepo;
    private final IReservationRepository _reservationRepo;
    private final IOrderRepository _orderRepo;
    private final IUserRepository _userRepo;
    private final IOrderServices _orderServices;

    @Override
    @Transactional
    @Auditable(action = "CREATE_RESERVATION")
    public ResultHandler<ReservationResponse> create(ReservationRequest request, String userToken) {
        Duration duration = Duration.between(request.getStartTime(), request.getEndTime());

        if (duration.toMinutes() < 30)
            return ResultHandler.failure(
                    "Reservation must be at least 30 minutes long",
                    HttpStatus.BAD_REQUEST.value()
            );

        if (duration.toHours() > 3)
            return ResultHandler.failure(
                    "Reservation cannot exceed 3 hours",
                    HttpStatus.BAD_REQUEST.value()
            );

        if (!_tableRepo.isTableExist(request.getTableToken()))
            return ResultHandler.failure(
                    "Table not found",
                    HttpStatus.NOT_FOUND.value()
            );

        boolean isFreeInTimeframe = _tableRepo.findAllTables("pl", request.getStartTime(), request.getEndTime())
                .stream()
                .anyMatch(table -> table.getToken().equals(request.getTableToken()));

        if (!isFreeInTimeframe)
            return ResultHandler.failure(
                    "Table is already reserved in this timeframe",
                    HttpStatus.CONFLICT.value()
            );

        String newReservationToken = _reservationRepo.createReservation(request, userToken);

        if (newReservationToken == null)
            return ResultHandler.failure(
                    "Failed to create reservation",
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );

        var orderCreate = _orderServices.createOrderForReservation(
                newReservationToken,
                request.getTableToken(),
                request.getDishes());

        if (orderCreate == null)
            return ResultHandler.failure(
                    "Order can't create",
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );

        ReservationResponse response = new ReservationResponse();
        response.setActive(true);

        List<ReservationDishResponse> dishResponses = orderCreate.dishes().stream().map(
                domainDish -> {
                    ReservationDishResponse dishRes = new ReservationDishResponse();
                    dishRes.setDishName(domainDish.dishName());
                    dishRes.setPrice(domainDish.price());
                    dishRes.setQuantity(domainDish.quantity());
                    return dishRes;
                }
        ).toList();

        response.setDishes(dishResponses);
        response.setTotalPrice(orderCreate.totalPrice());

        return ResultHandler.success(
                "Reservation created successfully",
                HttpStatus.CREATED.value(),
                response
        );
    }

    @Override
    public ResultHandler<PagedResult<ClientReservationResponse>> history(ClientReservationRequest request, PaggedRequest pagged, String userToken) {
        String lang = LocaleContextHolder.getLocale().getLanguage();

        var response = _reservationRepo.history(userToken, lang, request, pagged);

        return ResultHandler.success(
                "User reservations retrieved successfully",
                HttpStatus.OK.value(),
                response
        );
    }

    @Override
    public ResultHandler<ReservationDetailsResponse> details(String reservationToken, String userToken) {
        String lang = LocaleContextHolder.getLocale().getLanguage();

        ReservationDetailsResponse response = _reservationRepo.details(reservationToken, userToken, lang);

        var orderSummary = _orderServices.getOrderSummaryForReservation(reservationToken);

        response.setTotalPrice(orderSummary.totalPrice());
        response.setDishes(orderSummary.dishes());

        return ResultHandler.success(
                "Reservation details retrieved successfully",
                HttpStatus.OK.value(),
                response
        );
    }

    @Override
    public ResultHandler<Void> cancel(String reservationToken, String userToken) {
        _reservationRepo.cancel(reservationToken, userToken);

        return ResultHandler.success(
                "Reservation cancelled successfully",
                HttpStatus.OK.value()
        );
    }

    @Override
    public ResultHandler<PagedResult<TodayReservationsResponse>> today(PaggedRequest request) {
        String lang = LocaleContextHolder.getLocale().getLanguage();

        var response = _reservationRepo.today(lang, request);

        for (TodayReservationsResponse res : response.getItems()) {
            var orderDetails = _orderServices.todayOrderDetails(res.getToken(), lang);
            res.setTotalPrice(orderDetails.totalPrice());
            res.setDishes(orderDetails.dishes());
        }

        return ResultHandler.success(
                "Today's reservations retrieved successfully",
                HttpStatus.OK.value(),
                response
        );
    }

    @Override
    public ResultHandler<Void> removeItemFromReservation(String waiterToken, String reservationToken, ReservationDishRequest request) {
        _orderServices.removeItemFromReservation(waiterToken, reservationToken, request);

        return ResultHandler.success(
                "Order item removed successfully",
                HttpStatus.OK.value()
        );
    }

    @Override
    public ResultHandler<Void> addItemFromReservation(String waiterToken, String reservationToken, List<ReservationDishRequest> request) {
        _orderServices.addItemFromReservation(waiterToken, reservationToken, request);

        return ResultHandler.success(
                "Order items add successfully",
                HttpStatus.OK.value()
        );
    }

    @Override
    public ResultHandler<Void> assignWaiter(String reservationToken, String waiterToken) {
        if (!_userRepo.isInRole("ROLE_WAITER", waiterToken))
            return ResultHandler.failure(
                    "Not waiter",
                    HttpStatus.UNAUTHORIZED.value()
            );

        _reservationRepo.active(reservationToken);
        _orderRepo.assignWaiterToOrders(reservationToken, waiterToken);

        return ResultHandler.success(
                "Waiters assigned successfully",
                HttpStatus.OK.value()
        );
    }

    @Transactional
    @Override
    public ResultHandler<Void> isAbsent(String reservationToken) {
        _reservationRepo.isAbsent(reservationToken);
        _orderRepo.isAbsent(reservationToken);

        return ResultHandler.success(
                "Orders absent successfully",
                HttpStatus.OK.value()
        );
    }
}
