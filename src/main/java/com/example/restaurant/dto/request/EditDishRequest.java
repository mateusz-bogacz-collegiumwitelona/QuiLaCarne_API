package com.example.restaurant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class EditDishRequest {
  @NotBlank(message = "Dish token is required")
  private String dishToken;

  private String newName;

  private String categoryToken;

  @Positive(message = "Price must be greater than 0")
  private Integer price;

  List<String> ingredientTokens = new ArrayList<>();

  private MultipartFile photo;
}
