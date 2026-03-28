package com.example.restaurant.repository.interfaces;

import com.example.restaurant.dto.request.ClientReservationRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.request.ReservationRequest;
import com.example.restaurant.dto.response.ClientReservationResponse;
import com.example.restaurant.dto.response.ReservationDetailsResponse;
import com.example.restaurant.dto.response.TodayReservationsResponse;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.models.Reservations;

public interface IReservationRepository {
    String createReservation(ReservationRequest request, String userToken);

    PagedResult<ClientReservationResponse> history(String userToken, String lang, ClientReservationRequest filter, PaggedRequest pagged);

    ReservationDetailsResponse details(String reservationToken, String userToken, String lang);

    void cancel(String reservationToken, String userToken);

    PagedResult<TodayReservationsResponse> today(String lang, PaggedRequest pagged);

    void active(String reservationToken);

    void isAbsent(String reservationToken);

    Reservations findByToken(String token);
}
