package com.example.restaurant.dto.request;

import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import lombok.Data;

@Data
public class DishFilterRequest {
  @Parameter(description = "List of allergen tokens to exclude (e.g., GLUTEN, LACTOSE)")
  private List<String> excludedAllergens;
}
