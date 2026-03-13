package com.example.restaurant.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class ReservationDetailsResponse {
    private String status;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private List<ReservationDishResponse> dishes;
    private int totalPrice;
}
