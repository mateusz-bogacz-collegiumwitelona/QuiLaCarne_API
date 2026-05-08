package com.example.restaurant.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
