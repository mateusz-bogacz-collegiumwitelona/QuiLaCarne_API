package com.example.restaurant.dto.sync;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncIngredientResponse {
  private String token;
  private String nameEn;
  private String namePl;
  private List<String> allergenTokens;
}
