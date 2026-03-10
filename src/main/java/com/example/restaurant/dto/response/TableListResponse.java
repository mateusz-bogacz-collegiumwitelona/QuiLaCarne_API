package com.example.restaurant.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class TableListResponse {
    private String token;
    private int tableNumber;
    private int capacity;
    private String status;
    private OffsetDateTime updatedAt;
}
