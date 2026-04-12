package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.request.ClientReservationRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.request.ReservationDishRequest;
import com.example.restaurant.dto.request.ReservationRequest;
import com.example.restaurant.dto.response.ClientReservationResponse;
import com.example.restaurant.dto.response.DictionaryResponse;
import com.example.restaurant.dto.response.ReservationDetailsResponse;
import com.example.restaurant.dto.response.ReservationResponse;
import com.example.restaurant.helpers.PagedResult;

import java.util.List;

public interface IReservationServices {
    ReservationResponse create(ReservationRequest request, String userToken);

    PagedResult<ClientReservationResponse> history(ClientReservationRequest request, PaggedRequest pagged, String userToken);

    ReservationDetailsResponse details(String reservationToken, String userToken);

    void cancel(String reservationToken, String userToken);

    void removeItemFromReservation(String userToken, String reservationToken, ReservationDishRequest request);

    void addItemFromReservation(String userToken, String reservationToken, List<ReservationDishRequest> request);

    void assignWaiter(String reservationToken, String waiterToken);

    void isAbsent(String reservationToken);

    DictionaryResponse getDictionary();
}
