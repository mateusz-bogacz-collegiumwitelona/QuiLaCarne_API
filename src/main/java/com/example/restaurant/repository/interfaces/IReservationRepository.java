package com.example.restaurant.repository.interfaces;

import com.example.restaurant.dto.request.ReservationRequest;
import com.example.restaurant.dto.response.ClientReservationResponse;

import java.util.List;

public interface IReservationRepository {
    String createReservation(ReservationRequest request, String userToken);

    List<ClientReservationResponse> history(String userToken, String lang);
}
