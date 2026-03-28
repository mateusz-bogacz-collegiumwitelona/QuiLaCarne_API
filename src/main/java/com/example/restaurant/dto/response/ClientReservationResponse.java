package com.example.restaurant.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ClientReservationResponse {
    private String token;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private String status;
}
