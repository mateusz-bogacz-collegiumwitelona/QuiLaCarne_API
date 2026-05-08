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
public class DishResponse {
  private String name;
  private int price;
  private String imageUrl;
  private List<String> ingridents;
  private List<String> allergens;
}
