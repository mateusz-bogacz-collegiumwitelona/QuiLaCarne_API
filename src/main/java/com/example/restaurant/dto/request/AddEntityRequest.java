package com.example.restaurant.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddEntityRequest {
  @NotBlank(message = "Polish name cannot be blank")
  private String namePl;

  @NotBlank(message = "English name cannot be blank")
  private String nameEn;
}
