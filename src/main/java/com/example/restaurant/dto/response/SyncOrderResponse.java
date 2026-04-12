package com.example.restaurant.dto.response;

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
public class SyncOrderResponse {
    private String token;
    private String reservationToken;
    private String tableToken;
    private String waiterToken;
    private List<String> statusTokens;
    private int totalPrice;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}