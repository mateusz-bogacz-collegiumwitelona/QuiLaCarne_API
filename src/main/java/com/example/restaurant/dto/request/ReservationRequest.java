package com.example.restaurant.dto.request;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class ReservationRequest {
    private List<ReservationDishRequest> dishes;
    private String tableToken;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
}
