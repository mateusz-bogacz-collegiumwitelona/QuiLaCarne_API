package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.request.ReservationRequest;
import com.example.restaurant.dto.response.ClientReservationResponse;
import com.example.restaurant.dto.response.ReservationResponse;
import com.example.restaurant.helpers.ResultHandler;

import java.util.List;

public interface IReservationServices {
    ResultHandler<ReservationResponse> create(ReservationRequest request, String userToken);

    ResultHandler<List<ClientReservationResponse>> history(String userToken);
}
