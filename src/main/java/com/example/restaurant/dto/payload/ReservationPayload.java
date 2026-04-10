package com.example.restaurant.dto.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationPayload {
    private String token;
    private String userToken;
    private String tableToken;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private List<String> statusTokens;
}