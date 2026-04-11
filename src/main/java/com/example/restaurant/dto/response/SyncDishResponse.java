package com.example.restaurant.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncDishResponse {
    private String token;
    private String name;
    private int price;
    private boolean isAvailable;
    private String unavailableReason;
    private String imageUrl;
    private String categoryToken;
    private List<String> ingredientTokens;
}