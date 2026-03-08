package com.example.restaurant.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DishListResponse {
    private String token;
    private String name;
    private float price;
    private boolean isActive;
    private String categoryName;
    private List<IngredientListResponse>  ingredients;
}
