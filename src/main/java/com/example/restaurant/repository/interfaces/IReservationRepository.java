package com.example.restaurant.repository.interfaces;

import com.example.restaurant.dto.request.ClientReservationRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.request.ReservationRequest;
import com.example.restaurant.dto.response.ClientReservationResponse;
import com.example.restaurant.helpers.PagedResult;

public interface IReservationRepository {
    String createReservation(ReservationRequest request, String userToken);

    PagedResult<ClientReservationResponse> history(String userToken, String lang, ClientReservationRequest filter, PaggedRequest pagged);
}
