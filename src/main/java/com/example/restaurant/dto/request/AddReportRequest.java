package com.example.restaurant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddReportRequest {
  @NotBlank(message = "Client token is required")
  private String clientToken;

  @NotBlank(message = "Reason cannot be empty")
  @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
  private String reason;
}
