package com.example.restaurant.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientReservationResponse {
    private String token;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private String status;
}
