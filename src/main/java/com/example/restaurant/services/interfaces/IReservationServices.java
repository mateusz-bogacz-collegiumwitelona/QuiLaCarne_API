package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.request.ClientReservationRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.request.ReservationDishRequest;
import com.example.restaurant.dto.request.ReservationRequest;
import com.example.restaurant.dto.response.ClientReservationResponse;
import com.example.restaurant.dto.response.ReservationDetailsResponse;
import com.example.restaurant.dto.response.ReservationResponse;
import com.example.restaurant.dto.response.TodayReservationsResponse;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.helpers.ResultHandler;

public interface IReservationServices {
    ResultHandler<ReservationResponse> create(ReservationRequest request, String userToken);

    ResultHandler<PagedResult<ClientReservationResponse>> history(ClientReservationRequest request, PaggedRequest pagged, String userToken);

    ResultHandler<ReservationDetailsResponse> details(String reservationToken, String userToken);

    ResultHandler<Boolean> cancel(String reservationToken, String userToken);

    ResultHandler<PagedResult<TodayReservationsResponse>> today(PaggedRequest request);

    ResultHandler<Boolean> removeItemFromReservation(String userToken, String reservationToken, ReservationDishRequest request);
}
