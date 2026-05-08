package com.example.restaurant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangeDishAvailableRequest {
  @NotBlank(message = "Token is required")
  private String token;

  private boolean isAvailable;

  @Size(max = 500, message = "Unavailable reason cannot exceed 500 characters")
  private String unavailableReason;
}
