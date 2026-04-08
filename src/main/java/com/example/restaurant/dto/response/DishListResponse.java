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
public class DishListResponse {
    private String token;
    private String name;
    private float price;
    private boolean isActive;
    private String imageUrl;
    private String categoryName;
    private List<IngredientListResponse> ingredients;
}
