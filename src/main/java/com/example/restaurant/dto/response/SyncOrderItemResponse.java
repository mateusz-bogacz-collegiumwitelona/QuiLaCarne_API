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
public class SyncOrderItemResponse {
    private String token;
    private String orderToken;
    private String productToken;
    private List<String> statusTokens;
    private int quantity;
    private int priceAtTimeOfOrder;
    private String note;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}