package com.example.restaurant.dto.request;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

import java.util.List;

@Data
public class DishFilterRequest {
    @Parameter(description = "List of allergen tokens to exclude (e.g., GLUTEN, LACTOSE)")
    private List<String> excludedAllergens;
}
