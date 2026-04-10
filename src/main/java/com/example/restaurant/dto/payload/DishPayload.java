package com.example.restaurant.dto.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DishPayload {
    private String token;
    private String name;
    private Integer price;
    private boolean isAvailable;
    private String unavailableReason;
    private String imageUrl;
    private String categoryToken;
    private List<String> ingredientTokens;
}
