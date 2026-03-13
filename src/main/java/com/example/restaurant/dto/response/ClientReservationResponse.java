package com.example.restaurant.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class ClientReservationResponse {
    private String token;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private String status;
}
