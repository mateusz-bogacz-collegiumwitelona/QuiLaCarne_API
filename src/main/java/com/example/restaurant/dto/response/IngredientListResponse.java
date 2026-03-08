package com.example.restaurant.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class IngredientListResponse {
    private String token;
    private String name;
    private List<String> allergens;
}
