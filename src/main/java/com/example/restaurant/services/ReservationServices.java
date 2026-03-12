package com.example.restaurant.services;

import com.example.restaurant.dto.request.ReservationRequest;
import com.example.restaurant.dto.response.ReservationDishResponse;
import com.example.restaurant.dto.response.ReservationResponse;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.repository.interfaces.IOrderRepository;
import com.example.restaurant.repository.interfaces.IReservationRepository;
import com.example.restaurant.repository.interfaces.ITableRespository;
import com.example.restaurant.services.interfaces.IReservationServices;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationServices implements IReservationServices {
    private final ITableRespository _tableRepo;
    private final IReservationRepository _reservationRepo;
    private final IOrderRepository  _orderRepo;

    @Transactional
    public ResultHandler<ReservationResponse> create(ReservationRequest request, String userToken) {
        try {
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

            var orderCreate = _orderRepo.createOrderForReservation(
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
        catch (Exception ex)
        {
            return ResultHandler.failure(
                    ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
        }
    }
}
