package com.example.restaurant.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableListResponse {
    private String token;
    private int tableNumber;
    private int capacity;
    private String status;
    private OffsetDateTime updatedAt;
}
