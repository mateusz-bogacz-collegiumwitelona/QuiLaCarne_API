package com.example.restaurant.dto.request;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Valid
public class PaggedRequest {
  @Parameter(description = "Page number (Default 1)")
  @Min(value = 1, message = "Page number must be greater than 0")
  private int page = 1;

  @Parameter(description = "Number of item per page (Default 10)")
  @Min(value = 1, message = "Page number must be greater than 0")
  private int size = 10;
}
