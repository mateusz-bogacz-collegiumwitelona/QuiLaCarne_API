package com.example.restaurant.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class TableListResponse {
    private String token;
    private int tableNuber;
    private int capacity;
    private String status;
    private OffsetDateTime updatedAt;
}
